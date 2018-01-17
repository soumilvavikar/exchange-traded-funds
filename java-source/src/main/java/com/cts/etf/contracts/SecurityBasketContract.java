package com.cts.etf.contracts;

import com.cts.etf.SecurityBasket;
import com.google.common.collect.Sets;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
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

public class SecurityBasketContract implements Contract {

    public static final String SECURITY_BASKET_CONTRACT_ID = "com.cts.etf.contracts.SecurityBasketContract";

    public interface Commands extends CommandData {
        class Issue extends TypeOnlyCommandData implements SecurityBasketContract.Commands {
        }

        class Transfer extends TypeOnlyCommandData implements SecurityBasketContract.Commands {
        }

        class Settle extends TypeOnlyCommandData implements SecurityBasketContract.Commands {
        }

        class Iou extends TypeOnlyCommandData implements SecurityBasketContract.Commands {
        }
    }
    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<SecurityBasketContract.Commands> command = requireSingleCommand(tx.getCommands(), SecurityBasketContract.Commands.class);
        final SecurityBasketContract.Commands commandData = command.getValue();
        final Set<PublicKey> setOfSigners = new HashSet<>(command.getSigners());
        if (commandData instanceof SecurityBasketContract.Commands.Issue) {
            verifyIssue(tx, setOfSigners);
        } else if (commandData instanceof SecurityBasketContract.Commands.Transfer) {
            verifyTransfer(tx, setOfSigners);
        } else if (commandData instanceof SecurityBasketContract.Commands.Settle) {
            verifySettle(tx, setOfSigners);
        } else if (commandData instanceof SecurityBasketContract.Commands.Iou) {
            verifyIou(tx, setOfSigners);
        } else {
            throw new IllegalArgumentException("Unrecognised command.");
        }
    }

    private Set<PublicKey> keysFromParticipants(SecurityBasket obligation) {
        return obligation
                .getParticipants().stream()
                .map(AbstractParty::getOwningKey)
                .collect(toSet());
    }

    // This only allows one securityBasket issuance per transaction.
    private void verifyIssue(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(req -> {
            req.using("No inputs should be consumed when issuing an securityBasket.",
                    tx.getInputStates().isEmpty());
            req.using("Only one securityBasket state should be created when issuing an securityBasket.", tx.getOutputStates().size() == 1);
            SecurityBasket securityBasket = (SecurityBasket) tx.getOutputStates().get(0);
            req.using("A newly issued securityBasket must have a Hash.", securityBasket.getBasketIpfsHash() != null);
            req.using("The lender and borrower must be the same identity.", securityBasket.getBorrower().equals(securityBasket.getLender()));
            req.using("Both lender and borrower together only may sign securityBasket issue transaction.",
                    signers.equals(keysFromParticipants(securityBasket)));
            return null;
        });
    }

    // This only allows one securityBasket transfer per transaction.
    private void verifyTransfer(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(req -> {
            req.using("An securityBasket transfer transaction should only consume one input state.", tx.getInputs().size() == 1);
            req.using("An securityBasket transfer transaction should only create one output state.", tx.getOutputs().size() == 1);
            SecurityBasket input = tx.inputsOfType(SecurityBasket.class).get(0);
            SecurityBasket output = tx.outputsOfType(SecurityBasket.class).get(0);
            req.using("Only the lender property may change.", input.withoutLender().equals(output.withoutLender()));
            req.using("The lender property must change in a transfer.", !input.getLender().equals(output.getLender()));
            req.using("The borrower, old lender and new lender only must sign an securityBasket transfer transaction",
                    signers.equals(Sets.union(keysFromParticipants(input), keysFromParticipants(output))));
            return null;
        });
    }

    private void verifyIou(LedgerTransaction tx, Set<PublicKey> signers) {

    }

    private void verifySettle(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(req -> {
            // Check for the presence of an input securityBasket state.
//            List<SecurityBasket> securityBasketInputs = tx.inputsOfType(SecurityBasket.class);
//            req.using("There must be one input securityBasket.", securityBasketInputs.size() == 1);

            // Check there are output cash states.
            // We don't care about cash inputs, the Cash contract handles those.
//            List<Cash.State> cash = tx.outputsOfType(Cash.State.class);
//            req.using("There must be output cash.", !cash.isEmpty());

            // Check that the cash is being assigned to us.
//            SecurityBasket inputSecurityBasket = securityBasketInputs.get(0);
//            List<Cash.State> acceptableCash = cash.stream().filter(it -> it.getOwner().equals(inputSecurityBasket.getLender())).collect(Collectors.toList());
//            req.using("There must be output cash paid to the recipient.", !acceptableCash.isEmpty());

            // Sum the cash being sent to us (we don't care about the issuer).
//            Amount<Currency> sumAcceptableCash = withoutIssuer(sumCash(acceptableCash));
//            Amount<Currency> amountOutstanding = inputSecurityBasket.getAmount().minus(inputSecurityBasket.getPaid());
//            req.using("The amount settled cannot be more than the amount outstanding.", amountOutstanding.compareTo(sumAcceptableCash) >= 0);

//            List<SecurityBasket> securityBasketOutputs = tx.outputsOfType(SecurityBasket.class);

            // Check to see if we need an output securityBasket or not.
            /*if (amountOutstanding.equals(sumAcceptableCash)) {
                // If the securityBasket has been fully settled then there should be no securityBasket output state.
                req.using("There must be no output securityBasket as it has been fully settled.", securityBasketOutputs.isEmpty());
            } else {*/
                // If the securityBasket has been partially settled then it should still exist.
//                req.using("There must be one output securityBasket.", securityBasketOutputs.size() == 1);

                // Check only the paid property changes.
//                SecurityBasket outputSecurityBasket = securityBasketOutputs.get(0);
//                req.using("The hash may not change when settling.", inputSecurityBasket.getBasketIpfsHash().equals(outputSecurityBasket.getBasketIpfsHash()));
//                req.using("The borrower may not change when settling.", inputSecurityBasket.getBorrower().equals(outputSecurityBasket.getBorrower()));
//                req.using("The lender may not change when settling.", inputSecurityBasket.getLender().equals(outputSecurityBasket.getLender()));
//                req.using("The linearId may not change when settling.", inputSecurityBasket.getLinearId().equals(outputSecurityBasket.getLinearId()));

                // Check the paid property is updated correctly.
//                req.using("Paid property incorrectly updated.", outputSecurityBasket.getPaid().equals(inputSecurityBasket.getPaid().plus(sumAcceptableCash)));
            /*}*/

            // Checks the required parties have signed.
//            req.using("Both lender and borrower together only must sign securityBasket settle transaction.", signers.equals(keysFromParticipants(inputSecurityBasket)));
            return null;
        });
    }
}
