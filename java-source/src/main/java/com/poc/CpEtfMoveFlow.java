package com.poc;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.OwnableState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

public class CpEtfMoveFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private final String etfCode;
        private final Party owner;
        private final Party newOwner;
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


        public Initiator(String etfCode,
                         Party owner,
                         Party newOwner,
                         Party notary,
                         Boolean anonymous) {
            this.etfCode = etfCode;
            this.owner = owner;
            this.newOwner = newOwner;
            this.notary = notary;
            this.anonymous = anonymous;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            CpVaultManager vaultManager = new CpVaultManager((getServiceHub().getVaultService()));

            StateAndRef<CpEtfState> etf = vaultManager.getEtfs(etfCode);
            if (etf == null) {
                throw new FlowException(String.format("The ETF %s has already been consumed!.", etfCode));
            }

            OwnableState outputState = etf.getState().getData().withNewOwner(newOwner).getOwnableState();

            final PublicKey ourSigningKey = this.owner.getOwningKey();


            final TransactionBuilder utx =
                    new TransactionBuilder(this.notary)
                            .addInputState(etf)
                            .addOutputState(outputState, CpEtf.CP_ETF_ID)
                            .addCommand(
                                    new CpEtf.Commands.Move(),
                                    ourSigningKey);



            // Step 2. Building.

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

    @InitiatedBy(CpEtfMoveFlow.Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public Responder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final SignedTransaction stx =
                    subFlow(new CpEtfMoveFlow.SignTxFlowNoChecking(otherFlow,
                            SignTransactionFlow.Companion.tracker()));
            return waitForLedgerCommit(stx.getId());
        }
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
