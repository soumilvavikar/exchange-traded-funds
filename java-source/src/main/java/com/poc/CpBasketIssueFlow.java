package com.poc;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.etf.SecurityBasket;
import com.cts.etf.contracts.SecurityBasketContract;
import com.cts.etf.flows.EtfBaseFlow;
import com.cts.etf.flows.IssueSecurityBasketFlow;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CpBasketIssueFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final String basketIpfsHash;
        private final Party owner;
        private final Party notary;
        private final Boolean anonymous;

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


        public Initiator(String basketIpfsHash,
                         Party owner,
                         Party notary,
                         Boolean anonymous) {
            this.basketIpfsHash = basketIpfsHash;
            this.owner = owner;
            this.notary = notary;
            this.anonymous = anonymous;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            final State state = new State(this.owner, basketIpfsHash);
            final PublicKey ourSigningKey = this.owner.getOwningKey();

            // Step 2. Building.

            final List<PublicKey> requiredSigners =
                    ImmutableList.of(this.owner.getOwningKey());

            StateAndContract stateAndContract = new StateAndContract(state, CpBasket.CP_BASKET_ID);

            final TransactionBuilder utx =
                    new TransactionBuilder(this.notary)
                            .withItems(stateAndContract)
                            .addCommand(
                                    new CpBasket.Commands.Issue(),
                                    this.owner.getOwningKey());

            // Step 3. Sign the transaction.

            final SignedTransaction ptx =
                    getServiceHub().signInitialTransaction(utx, ourSigningKey);

            final FlowSession lenderFlow = initiateFlow(owner);
            final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                    ptx,
                    ImmutableSet.of(lenderFlow),
                    ImmutableList.of(ourSigningKey),
                    COLLECTING.childProgressTracker())
            );


            Set<Party> recordTransactions = new HashSet<>();
            recordTransactions.add(notary);
            return subFlow(new FinalityFlow(stx, recordTransactions,
                    FINALISING.childProgressTracker()));
        }
    }

    @InitiatedBy(CpBasketIssueFlow.Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public Responder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final SignedTransaction stx =
                    subFlow(new SignTxFlowNoChecking(otherFlow,
                            SignTransactionFlow.Companion.tracker()));
            return waitForLedgerCommit(stx.getId());
        }

        static class SignTxFlowNoChecking extends SignTransactionFlow {
            SignTxFlowNoChecking(FlowSession otherFlow, ProgressTracker progressTracker) {
                super(otherFlow, progressTracker);
            }

            @Override
            protected void checkTransaction(SignedTransaction tx) {
                // TODO: Add checking here.
            }
        }
    }
}