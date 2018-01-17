package com.cts.etf.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.etf.CreateEtfRequest;
import com.cts.etf.SecurityBasket;
import com.cts.etf.contracts.CreateEtfRequestContract;
import com.cts.vault.VaultManager;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.SwapIdentitiesFlow;
import net.corda.core.contracts.StateAndRef;
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

public class EtfCreationRequestFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends EtfBaseFlow {
        private final String basketIpfsHash;
        private final Party etfSponsorer;
        private final String etfCode;
        private final Boolean anonymous;

        private final ProgressTracker.Step INITIALISING = new ProgressTracker.Step("Performing initial steps.");
        private final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Performing initial steps.");
        private final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing transaction.");
        private final ProgressTracker.Step COLLECTING = new ProgressTracker.Step("Collecting counterparty signature.") {
            @Override public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Finalising transaction.") {
            @Override public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                INITIALISING, BUILDING, SIGNING, COLLECTING, FINALISING
        );

        public Initiator(String basketIpfsHash, String etfCode, Party etfSponsorer, Boolean anonymous) {
            this.basketIpfsHash = basketIpfsHash;
            this.etfSponsorer = etfSponsorer;
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
            // Step 1. Initialisation.
            progressTracker.setCurrentStep(INITIALISING);
            final CreateEtfRequest createEtfRequest = createEtfCreationRequest();
            final PublicKey ourSigningKey = createEtfRequest.getBorrower().getOwningKey();

            // Step 2. Building.
            progressTracker.setCurrentStep(BUILDING);
            final List<PublicKey> requiredSigners = createEtfRequest.getParticipantKeys();

            // TODO Validate Input
            VaultManager vaultManager = new VaultManager((getServiceHub().getVaultService()));
            StateAndRef<SecurityBasket> securityBasket = vaultManager.getSecurityBasket(basketIpfsHash);
            if (securityBasket == null) {
                throw new FlowException(String.format("The Security Basket %s has already been consumed!.", basketIpfsHash));
            }

            final TransactionBuilder utx = new TransactionBuilder(getFirstNotary())
                    .addOutputState(createEtfRequest, CreateEtfRequestContract.CREATE_ETF_REQUEST_CONTRACT_ID)
                    .addCommand(new CreateEtfRequestContract.Commands.Issue(), requiredSigners)
                    .setTimeWindow(getServiceHub().getClock().instant(), Duration.ofSeconds(30));

            // Step 3. Sign the transaction.
            progressTracker.setCurrentStep(SIGNING);
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(utx, ourSigningKey);

            // Step 4. Get the counter-party signature.
            progressTracker.setCurrentStep(COLLECTING);
            final FlowSession etfSponsorerFlow = initiateFlow(etfSponsorer);
            final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                    ptx,
                    ImmutableSet.of(etfSponsorerFlow),
                    ImmutableList.of(ourSigningKey),
                    COLLECTING.childProgressTracker())
            );

            // Step 5. Finalise the transaction.
            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(stx, FINALISING.childProgressTracker()));
        }

        @Suspendable
        private CreateEtfRequest createEtfCreationRequest() throws FlowException {
            if (anonymous) {
                final HashMap<Party, AnonymousParty> txKeys = subFlow(new SwapIdentitiesFlow(etfSponsorer));

                if (txKeys.size() != 2) {
                    throw new IllegalStateException("Something went wrong when generating confidential identities.");
                } else if (!txKeys.containsKey(getOurIdentity())) {
                    throw new FlowException("Couldn't create our conf. identity.");
                } else if (!txKeys.containsKey(etfSponsorer)) {
                    throw new FlowException("Couldn't create etfSponsorer's conf. identity.");
                }

                final AnonymousParty anonymousMe = txKeys.get(getOurIdentity());
                final AnonymousParty anonymousLender = txKeys.get(etfSponsorer);

                return new CreateEtfRequest(this.basketIpfsHash,this.etfCode, anonymousLender, anonymousMe);
            } else {
                return new CreateEtfRequest(this.basketIpfsHash,this.etfCode, getOurIdentity(),etfSponsorer);
            }
        }
    }

    @InitiatedBy(EtfCreationRequestFlow.Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public Responder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final SignedTransaction stx = subFlow(new EtfBaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
            return waitForLedgerCommit(stx.getId());
        }
    }
}
