package com.cts.etf;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ETFContract implements Contract {
	public static final String ETF_CONTRACT_ID =
			"com.cts.etf.ETFContract";

	public interface Commands extends CommandData {
		class SelfIssue extends TypeOnlyCommandData
				implements ETFContract.Commands {
		}

		class Issue extends TypeOnlyCommandData
				implements ETFContract.Commands {
		}

		class Transfer extends TypeOnlyCommandData implements
				ETFContract.Commands {
		}

		class Settle extends TypeOnlyCommandData implements
				ETFContract.Commands {
		}
	}

	@Override
	public void verify(LedgerTransaction tx) {
		final CommandWithParties<ETFContract.Commands> command =
				requireSingleCommand(tx.getCommands(),
						ETFContract.Commands.class);
		final ETFContract.Commands commandData = command.getValue();
		final Set<PublicKey> setOfSigners = new HashSet<>(command.getSigners());
		if (commandData instanceof ETFContract.Commands.SelfIssue) {
			verifySelfIssue(tx, setOfSigners);
		}
		else if (commandData instanceof ETFContract.Commands.Issue) {
			// TODO
			//verifyIssue(tx, setOfSigners);
		}
		else if (commandData instanceof ETFContract.Commands.Transfer) {
			// TODO
			// verifyTransfer(tx, setOfSigners);
		}
		else if (commandData instanceof ETFContract.Commands.Settle) {
			// TODO
			// verifySettle(tx, setOfSigners);
		}
		else {
			throw new IllegalArgumentException("Unrecognised command.");
		}
	}

	private Set<PublicKey> keysFromParticipants(ExchangeTradedFund etf) {
		return etf
				.getParticipants().stream()
				.map(AbstractParty::getOwningKey)
				.collect(toSet());
	}

	private void verifySelfIssue(LedgerTransaction tx,
			Set<PublicKey> signers) {
		requireThat(req -> {
			return null;
		});
	}

	// This only allows one obligation issuance per transaction.
	private void verifyIssue(LedgerTransaction tx, Set<PublicKey> signers) {
		requireThat(req -> {

			return null;
		});
	}
}
