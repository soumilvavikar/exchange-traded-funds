package com.poc;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.etf.SecurityBasket;
import com.cts.etf.contracts.SecurityBasketContract;
import com.cts.etf.flows.IssueSecurityBasketFlow;
import com.cts.vault.VaultManager;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.OwnableState;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.contracts.StateAndRef;
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

public class CpBasketMoveFlow {
@InitiatingFlow
@StartableByRPC
public static class Initiator extends FlowLogic<SignedTransaction> {
    private final String basketIpfsHash;
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


    public Initiator(String basketIpfsHash,
                     Party owner,
                     Party newOwner,
                     Party notary,
                     Boolean anonymous) {
        this.basketIpfsHash = basketIpfsHash;
        this.owner = owner;
        this.newOwner = newOwner;
        this.notary = notary;
        this.anonymous = anonymous;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        CpVaultManager vaultManager = new CpVaultManager((getServiceHub().getVaultService()));

        StateAndRef<State> securityBasket = vaultManager.getSecurityBasket(basketIpfsHash);
        if (securityBasket == null) {
            throw new FlowException(String.format("The Security Basket %s has already been consumed!.", basketIpfsHash));
        }

        OwnableState outputState = securityBasket.getState().getData().withNewOwner(newOwner).getOwnableState();

        final PublicKey ourSigningKey = this.owner.getOwningKey();


        final TransactionBuilder utx =
                new TransactionBuilder(this.notary)
                        .addInputState(securityBasket)
                        .addOutputState(outputState, CpBasket.CP_BASKET_ID)
                        .addCommand(
                                new CpBasket.Commands.Move(),
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

    @InitiatedBy(CpBasketMoveFlow.Initiator.class)
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
