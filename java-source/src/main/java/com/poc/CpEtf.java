package com.poc;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TimeWindow;
import net.corda.core.transactions.LedgerTransaction;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class CpEtf implements Contract {

    public static final String CP_ETF_ID =
            "com.poc.CpEtf";

    public static class Commands implements CommandData {
        public static class Move extends Commands {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof Move;
            }
        }

        public static class Issue extends Commands {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof Issue;
            }
        }
    }

    @Override
    public void verify(LedgerTransaction tx) {
        CommandWithParties<Commands> cmd = requireSingleCommand(tx.getCommands(), Commands.class);
        List<LedgerTransaction.InOutGroup<CpEtfState, CpEtfState>> groups = tx.groupStates(CpEtfState.class, CpEtfState::withoutOwner);
        TimeWindow timeWindow = tx.getTimeWindow();

        for (LedgerTransaction.InOutGroup group : groups) {
            List<CpEtfState> inputs = group.getInputs();
            List<CpEtfState> outputs = group.getOutputs();

            if (cmd.getValue() instanceof Commands.Move) {
                CpEtfState input = inputs.get(0);
                requireThat(require -> {
                    require.using("the transaction is signed by the owner of the CP", cmd.getSigners().contains(input.getOwner().getOwningKey()));
                    require.using("the state is propagated", outputs.size() == 1);
                    // Don't need to check anything else, as if outputs.size == 1 then the output is equal to
                    // the input ignoring the owner field due to the grouping.
                    return null;
                });

            }else if (cmd.getValue() instanceof Commands.Issue) {
                CpEtfState output = outputs.get(0);
                requireThat(require -> {
                    // Don't allow people to issue commercial paper under other entities identities.
                    require.using("output states are issued by a command signer", cmd.getSigners().contains(output.getOwner().getOwningKey()));
                    // Don't allow an existing CP state to be replaced by this issuance.
                    require.using("can't reissue an existing state", inputs.isEmpty());
                    return null;
                });
            } else {
                throw new IllegalArgumentException("Unrecognised command");
            }
        }
    }
}