package com.cts.etf.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.etf.CreateEtfRequest;
import com.cts.etf.ExchangeTradedFund;
import com.cts.etf.SecurityBasket;
import com.cts.etf.contracts.CreateEtfRequestContract;
import com.cts.vault.VaultManager;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.confidential.SwapIdentitiesFlow;
import net.corda.core.contracts.OwnableState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.TwoPartyDealFlow;
import net.corda.finance.flows.TwoPartyTradeFlow;

import java.security.PublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class EtfCreationRequestFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends EtfBaseFlow {
        private final String basketIpfsHash;
        private final Party etfSponsorer;
        private final String etfCode;
        private final int quantity;
        private final Boolean anonymous;

        private final ProgressTracker.Step INITIALISING = new ProgressTracker.Step("Performing initial steps.");
        private final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Performing initial steps.");
        private final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing transaction.");
        private final ProgressTracker.Step RECEIVING = new ProgressTracker.Step("Receiving transaction.");
        private final ProgressTracker.Step DEALING = new ProgressTracker.Step("Starting the deal flow.") {

            @Override
            public ProgressTracker childProgressTracker() {
                return TwoPartyDealFlow.Instigator.Companion.tracker();
            }

        };
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
                INITIALISING, BUILDING, SIGNING, COLLECTING, FINALISING, DEALING, RECEIVING
        );

        public Initiator(String basketIpfsHash, String etfCode, Party etfSponsorer, int quantity, Boolean anonymous) {
            this.basketIpfsHash = basketIpfsHash;
            this.etfSponsorer = etfSponsorer;
            this.etfCode = etfCode;
            this.quantity = quantity;
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
            // AP Signing Key
            final PublicKey ourSigningKey = createEtfRequest.getLender().getOwningKey();

            //TODO Add this later
            VaultManager vaultManager = new VaultManager((getServiceHub().getVaultService()));

            StateAndRef<SecurityBasket> securityBasket = vaultManager.getSecurityBasket(basketIpfsHash);
            if (securityBasket == null) {
                throw new FlowException(String.format("The Security Basket %s has already been consumed!.", basketIpfsHash));
            }
            StateAndRef<ExchangeTradedFund> issuedEtfs = vaultManager.getIssuedEtfs(etfCode);
            if (issuedEtfs == null) {
                throw new FlowException(String.format("No ETFs issued so far with code %s.", etfCode));
            }
            // TODO - Compare request quantity with issuee Etfs Quantity

            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);


            // Step 2. Building.
            progressTracker.setCurrentStep(BUILDING);

            final FlowSession etfSponsorerFlow = initiateFlow(etfSponsorer);

            List<StateAndRef<?>> list = new ArrayList<>();
            list.add(securityBasket);
            list.add(issuedEtfs);
            subFlow(new SendStateAndRefFlow(etfSponsorerFlow, list));

            System.out.println("In call before calling send().... ");
            etfSponsorerFlow.send(createEtfRequest); // TODO

            progressTracker.setCurrentStep(SIGNING);

            // Sync identities to ensure we know all of the identities involved in the transaction we're about to
            // be asked to sign
            subFlow(new IdentitySyncFlow.Receive(etfSponsorerFlow));

            SignTransactionFlow signedTransaction = new SignTransactionFlow(etfSponsorerFlow, progressTracker.getChildProgressTracker(DEALING)) {

                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    List<StateRef> stateRefs = stx.getTx().getInputs();
                    for(StateRef stateRef : stateRefs) {
                        System.out.println("Stateref - " + stateRef.toString());
                    }

                    // TODO Business logic to check for Etf Quantity
                }
            };

            SecureHash txId = subFlow(signedTransaction).getId();

            return waitForLedgerCommit(txId);

//            TwoPartyDealFlow.AutoOffer autoOffer = new TwoPartyDealFlow.AutoOffer(notary, createEtfRequest);
//            TwoPartyDealFlow.Instigator instigator = new TwoPartyDealFlow.Instigator(etfSponsorerFlow, autoOffer,
//                    progressTracker.getChildProgressTracker(DEALING));
//            return subFlow(instigator);

            /*
            final List<PublicKey> requiredSigners = createEtfRequest.getParticipantKeys();

            // TODO Validate Input


            final TransactionBuilder utx = new TransactionBuilder(getFirstNotary())
                    .addOutputState(createEtfRequest, CreateEtfRequestContract.CREATE_ETF_REQUEST_CONTRACT_ID)
                    .addCommand(new CreateEtfRequestContract.Commands.Issue(), requiredSigners)
                    .setTimeWindow(getServiceHub().getClock().instant(), Duration.ofSeconds(30));

            // Step 3. Sign the transaction.
            progressTracker.setCurrentStep(SIGNING);
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(utx, ourSigningKey);

            // Step 4. Get the counter-party signature.
            progressTracker.setCurrentStep(COLLECTING);

            final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                    ptx,
                    ImmutableSet.of(etfSponsorerFlow),
                    ImmutableList.of(ourSigningKey),
                    COLLECTING.childProgressTracker())
            );

            // Step 5. Finalise the transaction.
            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(stx, FINALISING.childProgressTracker()));
            */
        }

        @Suspendable
        private CreateEtfRequest createEtfCreationRequest() throws FlowException {
            /*if (anonymous) {
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

                return new CreateEtfRequest(this.basketIpfsHash, this.etfCode, anonymousLender, anonymousMe);
            } else {*/
                return new CreateEtfRequest(this.basketIpfsHash,this.etfCode, getOurIdentity(),etfSponsorer, quantity);
          /*  }*/
        }
    }

    @InitiatedBy(EtfCreationRequestFlow.Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        private final ProgressTracker.Step INITIALISING = new ProgressTracker.Step("Performing initial steps.");
        private final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Performing initial steps.");
        private final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing transaction.");
        private final ProgressTracker.Step RECEIVING = new ProgressTracker.Step("Receiving transaction.");
        private final ProgressTracker.Step DEALING = new ProgressTracker.Step("Starting the deal flow.") {

            @Override
            public ProgressTracker childProgressTracker() {
                return TwoPartyDealFlow.Instigator.Companion.tracker();
            }

        };
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
                INITIALISING, BUILDING, SIGNING, COLLECTING, FINALISING, DEALING, RECEIVING
        );

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        public Responder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            System.out.println("In Responder.... ");
            progressTracker.setCurrentStep(RECEIVING);
            StateAndRef<OwnableState> securityBasketState = subFlow(new ReceiveStateAndRefFlow<OwnableState>(otherFlow)).get(0);

            UntrustworthyData<SecurityBasket> untrustworthyData = otherFlow.receive(SecurityBasket.class);
            untrustworthyData.unwrap(null);
            SecurityBasket asset = (SecurityBasket)(securityBasketState.getState().getData());
            Party authParty = getServiceHub().getIdentityService().wellKnownPartyFromAnonymous(asset.getLender());
            if(!authParty.equals(otherFlow.getCounterparty())) {
                System.out.println("In Responder call().... AP and counter party of Sponserer is not same. ");
            }

            PublicKey owningKey = asset.getBorrower().getOwningKey();
            progressTracker.setCurrentStep(SIGNING);

            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            TransactionBuilder transactionBuilder = new TransactionBuilder(notary);


            final SignedTransaction stx = subFlow(new EtfBaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
            System.out.println("In Responder after signed trx.... ");
            return waitForLedgerCommit(stx.getId());
        }
    }
}
