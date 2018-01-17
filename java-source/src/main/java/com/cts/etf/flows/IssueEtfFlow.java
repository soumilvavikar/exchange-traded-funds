package com.cts.etf.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.etf.contracts.ETFContract;
import com.cts.etf.ExchangeTradedFund;
import com.cts.etf.api.EtfApi;
import net.corda.confidential.SwapIdentitiesFlow;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class IssueEtfFlow {
	@InitiatingFlow
	@StartableByRPC
	public static class Initiator extends EtfBaseFlow {
		private static final Logger logger =
				Logger.getLogger(EtfApi.class.toString());
		private final String etfName;
		private final String etfCode;
		private final int quantityOfEtf;
		private final Boolean anonymous;
		private final Party buyer;
		private final Party owner;

		// Constructor
		public Initiator(String etfName,String etfCode, int
				numberOfEtf, Party buyer, Party owner,
				Boolean
						anonymous) {
			this.etfName = etfName;
			this.etfCode = etfCode;
			this.quantityOfEtf = numberOfEtf;
			this.anonymous = anonymous;
			this.buyer = buyer;
			this.owner = owner;
		}

		@Suspendable
		@Override
		public SignedTransaction call() throws FlowException {
			logger.info("Inside call method of IssueEtfFlow");

			// Step 1. Initialisation.
			logger.info("INITIALISING");
			progressTracker.setCurrentStep(INITIALISING);
			ExchangeTradedFund exchangeTradedFund = createExchangeTradedFund();
			final PublicKey ourSigningKey = exchangeTradedFund.getBuyer()
					.getOwningKey();

			// Step 2. Building.
			logger.info("BUILDING");
			progressTracker.setCurrentStep(BUILDING);
			final List<PublicKey> requiredSigners = exchangeTradedFund
					.getParticipantKeys();

			final TransactionBuilder utx =
					new TransactionBuilder(getFirstNotary())
							.addOutputState(exchangeTradedFund, ETFContract
									.ETF_CONTRACT_ID)
							.addCommand(new ETFContract.Commands.SelfIssue(),
									requiredSigners)
							.setTimeWindow(getServiceHub().getClock().instant(),
									Duration
											.ofSeconds(30));

			// Step 3. Sign the transaction.
			logger.info("SIGNING");
			progressTracker.setCurrentStep(SIGNING);
			final SignedTransaction ptx =
					getServiceHub().signInitialTransaction(utx, ourSigningKey);

			// Step 4. Finalise the transaction.
			logger.info("FINALISING");
			progressTracker.setCurrentStep(FINALISING);
			return subFlow(new FinalityFlow(ptx, FINALISING.childProgressTracker()));
		}

		@Suspendable
		private ExchangeTradedFund createExchangeTradedFund()
				throws FlowException {
			logger.info("Inside createExchangeTradedFund");
			if (anonymous) {
				logger.info("Inside anonymous block");
				final HashMap<Party, AnonymousParty>
						txKeys = subFlow(new SwapIdentitiesFlow(owner));

				if (txKeys.size() != 2) {
					throw new IllegalStateException("Something went wrong when generating confidential identities.");
				} else if (!txKeys.containsKey(getOurIdentity())) {
					throw new FlowException("Couldn't create our conf. identity.");
				} else if (!txKeys.containsKey(owner)) {
					throw new FlowException("Couldn't create lender's conf. identity.");
				}

				final AnonymousParty anonymousMe = txKeys.get(getOurIdentity());
				final AnonymousParty anonymousLender = txKeys.get(owner);
				return new ExchangeTradedFund(etfCode, owner, buyer,
						etfName, quantityOfEtf);
			}
			return new ExchangeTradedFund(etfCode, owner, buyer,
					etfName, quantityOfEtf);
		}

		private final ProgressTracker.Step
				INITIALISING =
				new ProgressTracker.Step("Performing initial steps.");
		private final ProgressTracker.Step BUILDING =
				new ProgressTracker.Step("Performing initial steps.");
		private final ProgressTracker.Step SIGNING =
				new ProgressTracker.Step("Signing transaction.");
		private final ProgressTracker.Step
				FINALISING =
				new ProgressTracker.Step("Finalising transaction.") {
					@Override
					public ProgressTracker childProgressTracker() {
						return FinalityFlow.Companion.tracker();
					}
				};
		private final ProgressTracker progressTracker = new ProgressTracker(
				INITIALISING, BUILDING, SIGNING, FINALISING
		);

		@Override
		public ProgressTracker getProgressTracker() {
			return progressTracker;
		}
	}

	@InitiatedBy(IssueEtfFlow.Initiator.class)
	public static class Responder extends FlowLogic<SignedTransaction> {
		private final FlowSession otherFlow;

		public Responder(FlowSession otherFlow) {
			this.otherFlow = otherFlow;
		}

		@Suspendable
		@Override
		public SignedTransaction call() throws FlowException {
			final SignedTransaction stx =
					subFlow(new EtfBaseFlow.SignTxFlowNoChecking(otherFlow,
							SignTransactionFlow.Companion.tracker()));
			return waitForLedgerCommit(stx.getId());
		}
	}
}
