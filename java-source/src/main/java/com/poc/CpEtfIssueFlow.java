package com.poc;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CpEtfIssueFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private String etfName;
        private String etfCode;
        private int quantity;
        private Party owner;
        private Party notary;
        private Boolean anonymous;

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


        public Initiator(String etfCode,
                         String etfName,
                         int quantity,
                         Party owner,
                         Party notary,
                         Boolean anonymous) {
            this.etfCode = etfCode;
            this.etfName = etfName;
            this.quantity = quantity;
            this.owner = owner;
            this.notary = notary;
            this.anonymous = anonymous;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            final CpEtfState state = new CpEtfState(etfCode, etfName, quantity, owner);
            final PublicKey ourSigningKey = this.owner.getOwningKey();

            // Step 2. Building.

            final List<PublicKey> requiredSigners =
                    ImmutableList.of(this.owner.getOwningKey());

            StateAndContract stateAndContract = new StateAndContract(state, CpEtf.CP_ETF_ID);

            final TransactionBuilder utx =
                    new TransactionBuilder(this.notary)
                            .withItems(stateAndContract)
                            .addCommand(
                                    new CpEtf.Commands.Issue(),
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

    @InitiatedBy(CpEtfIssueFlow.Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public Responder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final SignedTransaction stx =
                    subFlow(new CpEtfIssueFlow.Responder.SignTxFlowNoChecking(otherFlow,
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
