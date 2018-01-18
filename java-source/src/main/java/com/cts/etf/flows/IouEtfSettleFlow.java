package com.cts.etf.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.etf.ExchangeTradedFund;
import com.cts.etf.contracts.ETFContract;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.contracts.asset.Cash;

import java.security.PublicKey;
import java.util.List;

public class IouEtfSettleFlow {

	@InitiatingFlow
	@StartableByRPC
	public static class Initiator extends EtfBaseFlow {
		private final UniqueIdentifier linearId;
		private final String etfCode;
		private final Boolean anonymous;

		private final ProgressTracker.Step PREPARATION =
				new ProgressTracker.Step("Obtaining IOU from vault.");
		private final ProgressTracker.Step BUILDING =
				new ProgressTracker.Step("Building and verifying transaction.");
		private final ProgressTracker.Step SIGNING =
				new ProgressTracker.Step("Signing transaction.");
		private final ProgressTracker.Step COLLECTING =
				new ProgressTracker.Step("Collecting counterparty signature.") {
					@Override
					public ProgressTracker childProgressTracker() {
						return CollectSignaturesFlow.Companion.tracker();
					}
				};
		private final ProgressTracker.Step FINALISING =
				new ProgressTracker.Step("Finalising transaction.") {
					@Override
					public ProgressTracker childProgressTracker() {
						return FinalityFlow.Companion.tracker();
					}
				};

		private final ProgressTracker progressTracker = new ProgressTracker(
				PREPARATION, BUILDING, SIGNING, COLLECTING, FINALISING
		);

		public Initiator(UniqueIdentifier linearId,
				String etfCode,
				Boolean anonymous) {
			this.linearId = linearId;
			this.etfCode = etfCode;
			this.anonymous = anonymous;
		}

		@Override
		public ProgressTracker getProgressTracker() {
			return progressTracker;
		}

		@Suspendable
		@Override
		public SignedTransaction call() throws FlowException {
			// Stage 1. Retrieve obligation specified by linearId from the vault.
			progressTracker.setCurrentStep(PREPARATION);
			final StateAndRef<ExchangeTradedFund> etfsToSettle =
					getEtfByLinearId(linearId);
			final ExchangeTradedFund inputEtf =
					etfsToSettle.getState().getData();

			// Stage 2. Resolve the lender and borrower identity if the obligation is anonymous.
			final Party authorizedParticipant =
					resolveIdentity(inputEtf.getBuyer());
			final Party sponsorer =
					resolveIdentity(inputEtf.getOwner());
			// Stage 3. This flow can only be initiated by the current recipient.
			if (!sponsorer.equals(getOurIdentity())) {
				throw new FlowException(
						"Settle ETF flow must be initiated by the sponsorer.");
			}

			// Stage 4. Create a settle command.
			final List<PublicKey> requiredSigners =
					inputEtf.getParticipantKeys();
			final Command
					settleCommand =
					new Command<>(new ETFContract.Commands.Settle(),
							requiredSigners);

			// Stage 5. Create a transaction builder. Add the settle command
			// and input obligation.
			progressTracker.setCurrentStep(BUILDING);
			final TransactionBuilder builder =
					new TransactionBuilder(getFirstNotary())
							.addInputState(etfsToSettle)
							.addCommand(settleCommand);

			// Stage 6. Get some cash from the vault and add a spend to our
			// transaction builder.
			final List<PublicKey> cashSigningKeys = Cash.generateSpend(
					getServiceHub(),
					builder,
					inputEtf.getFaceValue(),
					inputEtf.getOwner(),
					ImmutableSet.of()).getSecond();

			// Stage 7. Verify and sign the transaction.
			progressTracker.setCurrentStep(SIGNING);
			builder.verify(getServiceHub());
			final List<PublicKey> signingKeys =
					new ImmutableList.Builder<PublicKey>()
							.addAll(cashSigningKeys)
							.add(inputEtf.getBuyer()
									.getOwningKey())
							.build();
			final SignedTransaction ptx = getServiceHub()
					.signInitialTransaction(builder, signingKeys);

			// Stage 8. Get counterparty signature.
			progressTracker.setCurrentStep(COLLECTING);
			final FlowSession session = initiateFlow(sponsorer);
			subFlow(new IdentitySyncFlow.Send(session, ptx.getTx()));
			final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
					ptx,
					ImmutableSet.of(session),
					signingKeys,
					COLLECTING.childProgressTracker()));

			// Stage 9. Finalize the transaction.
			progressTracker.setCurrentStep(FINALISING);
			return subFlow(
					new FinalityFlow(stx, FINALISING.childProgressTracker()));
		}
	}

	@InitiatedBy(IouEtfSettleFlow.Initiator.class)
	public static class Responder extends FlowLogic<SignedTransaction> {
		private final FlowSession otherFlow;

		public Responder(FlowSession otherFlow) {
			this.otherFlow = otherFlow;
		}

		@Suspendable
		@Override
		public SignedTransaction call() throws FlowException {
			subFlow(new IdentitySyncFlow.Receive(otherFlow));
			SignedTransaction stx =
					subFlow(new EtfBaseFlow.SignTxFlowNoChecking(otherFlow,
							SignTransactionFlow.Companion.tracker()));
			return waitForLedgerCommit(stx.getId());
		}
	}
}
