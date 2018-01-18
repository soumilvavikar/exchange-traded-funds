package com.cts.etf.contracts;

import com.cts.etf.ExchangeTradedFund;
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
			"com.cts.etf.contracts.ETFContract";

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

		class Iou implements ETFContract.Commands {
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
		else if (commandData instanceof Commands.Iou) {
			verifyIou(tx, setOfSigners);
		}
		else if (commandData instanceof ETFContract.Commands.Transfer) {
			// TODO
			// verifyTransfer(tx, setOfSigners);
		}
		else if (commandData instanceof ETFContract.Commands.Settle) {
			verifySettle(tx, setOfSigners);
		}
		else {
			throw new IllegalArgumentException("Unrecognised command.");
		}
	}

	private void verifySettle(LedgerTransaction tx,
			Set<PublicKey> setOfSigners) {
		requireThat(req -> {
			return null;
		});
	}

	private void verifyIou(LedgerTransaction tx, Set<PublicKey> setOfSigners) {
		requireThat(req -> {
			return null;
		});
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
}
