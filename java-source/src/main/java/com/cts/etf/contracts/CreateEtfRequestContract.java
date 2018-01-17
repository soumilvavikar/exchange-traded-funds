package com.cts.etf.contracts;

import com.cts.etf.CreateEtfRequest;
import com.google.common.collect.Sets;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.examples.obligation.Obligation;
import net.corda.examples.obligation.ObligationContract;
import net.corda.finance.contracts.asset.Cash;

import java.security.PublicKey;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.contracts.Structures.withoutIssuer;
import static net.corda.finance.utils.StateSumming.sumCash;

public class CreateEtfRequestContract implements Contract {
    public static final String CREATE_ETF_REQUEST_CONTRACT_ID = "com.cts.etf.contracts.CreateEtfRequestContract";

    public interface Commands extends CommandData {
        class Issue extends TypeOnlyCommandData implements CreateEtfRequestContract.Commands {
        }

        class Transfer extends TypeOnlyCommandData implements CreateEtfRequestContract.Commands {
        }

        class Settle extends TypeOnlyCommandData implements CreateEtfRequestContract.Commands {
        }
    }

    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<CreateEtfRequestContract.Commands> command = requireSingleCommand(tx.getCommands(), CreateEtfRequestContract.Commands.class);
        final CreateEtfRequestContract.Commands commandData = command.getValue();
        final Set<PublicKey> setOfSigners = new HashSet<>(command.getSigners());
        if (commandData instanceof CreateEtfRequestContract.Commands.Issue) {
            verifyIssue(tx, setOfSigners);
        } else if (commandData instanceof CreateEtfRequestContract.Commands.Transfer) {
            //verifyTransfer(tx, setOfSigners);
        } else if (commandData instanceof CreateEtfRequestContract.Commands.Settle) {
           // verifySettle(tx, setOfSigners);
        } else {
            throw new IllegalArgumentException("Unrecognised command.");
        }
    }

    private Set<PublicKey> keysFromParticipants(CreateEtfRequest createEtfRequest) {
        return createEtfRequest
                .getParticipants().stream()
                .map(AbstractParty::getOwningKey)
                .collect(toSet());
    }

    // This only allows one ETF Creation Request per transaction.
    private void verifyIssue(LedgerTransaction tx, Set<PublicKey> signers) {
        System.out.println("In verifyIssue.... ");
        requireThat(req -> {
            req.using("No inputs should be consumed when issuing an ETF Creation Request.",
                    tx.getInputStates().isEmpty());
            req.using("Only one ETF Creation Request state should be created when issuing an ETF Creation Request.", tx.getOutputStates().size() == 1);
            CreateEtfRequest createEtfRequest = (CreateEtfRequest) tx.getOutputStates().get(0);
            req.using("A newly issued ETF Creation Request must have a Security Basket ID.", createEtfRequest.getBasketIpfsHash() != null);
            req.using("A newly issued ETF Creation Request must have a Valid ETF Code.", createEtfRequest.getEtfCode() != null);
            req.using("A newly issued ETF Creation Request must have a Valid Quantity.", createEtfRequest.getQuantity() > 0);
            req.using("The lender and borrower cannot be the same identity.", !createEtfRequest.getBorrower().equals(createEtfRequest.getLender()));
            req.using("Both lender and borrower together only may sign ETF Creation Request issue transaction.",
                    signers.equals(keysFromParticipants(createEtfRequest)));
            return null;
        });
    }

    // This only allows one obligation transfer per transaction.
//    private void verifyTransfer(LedgerTransaction tx, Set<PublicKey> signers) {
//        requireThat(req -> {
//            req.using("An obligation transfer transaction should only consume one input state.", tx.getInputs().size() == 1);
//            req.using("An obligation transfer transaction should only create one output state.", tx.getOutputs().size() == 1);
//            Obligation input = tx.inputsOfType(Obligation.class).get(0);
//            Obligation output = tx.outputsOfType(Obligation.class).get(0);
//            req.using("Only the lender property may change.", input.withoutLender().equals(output.withoutLender()));
//            req.using("The lender property must change in a transfer.", !input.getLender().equals(output.getLender()));
//            req.using("The borrower, old lender and new lender only must sign an obligation transfer transaction",
//                    signers.equals(Sets.union(keysFromParticipants(input), keysFromParticipants(output))));
//            return null;
//        });
//    }

//    private void verifySettle(LedgerTransaction tx, Set<PublicKey> signers) {
//        requireThat(req -> {
//            // Check for the presence of an input obligation state.
//            List<Obligation> obligationInputs = tx.inputsOfType(Obligation.class);
//            req.using("There must be one input obligation.", obligationInputs.size() == 1);
//
//            // Check there are output cash states.
//            // We don't care about cash inputs, the Cash contract handles those.
//            List<Cash.State> cash = tx.outputsOfType(Cash.State.class);
//            req.using("There must be output cash.", !cash.isEmpty());
//
//            // Check that the cash is being assigned to us.
//            Obligation inputObligation = obligationInputs.get(0);
//            List<Cash.State> acceptableCash = cash.stream().filter(it -> it.getOwner().equals(inputObligation.getLender())).collect(Collectors.toList());
//            req.using("There must be output cash paid to the recipient.", !acceptableCash.isEmpty());
//
//            // Sum the cash being sent to us (we don't care about the issuer).
//            Amount<Currency> sumAcceptableCash = withoutIssuer(sumCash(acceptableCash));
//            Amount<Currency> amountOutstanding = inputObligation.getAmount().minus(inputObligation.getPaid());
//            req.using("The amount settled cannot be more than the amount outstanding.", amountOutstanding.compareTo(sumAcceptableCash) >= 0);
//
//            List<Obligation> obligationOutputs = tx.outputsOfType(Obligation.class);
//
//            // Check to see if we need an output obligation or not.
//            if (amountOutstanding.equals(sumAcceptableCash)) {
//                // If the obligation has been fully settled then there should be no obligation output state.
//                req.using("There must be no output obligation as it has been fully settled.", obligationOutputs.isEmpty());
//            } else {
//                // If the obligation has been partially settled then it should still exist.
//                req.using("There must be one output obligation.", obligationOutputs.size() == 1);
//
//                // Check only the paid property changes.
//                Obligation outputObligation = obligationOutputs.get(0);
//                req.using("The amount may not change when settling.", inputObligation.getAmount().equals(outputObligation.getAmount()));
//                req.using("The borrower may not change when settling.", inputObligation.getBorrower().equals(outputObligation.getBorrower()));
//                req.using("The lender may not change when settling.", inputObligation.getLender().equals(outputObligation.getLender()));
//                req.using("The linearId may not change when settling.", inputObligation.getLinearId().equals(outputObligation.getLinearId()));
//
//                // Check the paid property is updated correctly.
//                req.using("Paid property incorrectly updated.", outputObligation.getPaid().equals(inputObligation.getPaid().plus(sumAcceptableCash)));
//            }
//
//            // Checks the required parties have signed.
//            req.using("Both lender and borrower together only must sign obligation settle transaction.", signers.equals(keysFromParticipants(inputObligation)));
//            return null;
//        });
//    }

}
