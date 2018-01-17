package com.cts.etf;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.examples.obligation.Obligation;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.corda.core.utilities.EncodingUtils.toBase58String;

public class SecurityBasket implements LinearState {

	private final Amount<Currency> faceValue =
			new Amount<>((long) 100, Currency.getInstance("INR"));
	private final String basketIpfsHash;
	private final AbstractParty lender; // AP
	private final AbstractParty borrower;
			// This can be thrid party place holder
	private final UniqueIdentifier linearId;

	public SecurityBasket(String basketIpfsHash,
			AbstractParty lender,
			AbstractParty borrower,
			UniqueIdentifier linearId) {
		this.basketIpfsHash = basketIpfsHash;
		this.lender = lender;
		this.borrower = borrower;
		this.linearId = linearId;
	}

	public SecurityBasket(String basketIpfsHash,
			AbstractParty lender,
			AbstractParty borrower) {
		this.basketIpfsHash = basketIpfsHash;
		this.lender = lender;
		this.borrower = borrower;
		this.linearId = new UniqueIdentifier();
	}

	public String getBasketIpfsHash() {
		return basketIpfsHash;
	}

	public AbstractParty getLender() {
		return lender;
	}

	public AbstractParty getBorrower() {
		return borrower;
	}

	public Amount<Currency> getFaceValue() {
		return faceValue;
	}

	@Override
	public UniqueIdentifier getLinearId() {
		return linearId;
	}

	@Override
	public List<AbstractParty> getParticipants() {
		return ImmutableList.of(lender, borrower);
	}

	public SecurityBasket withNewLender(AbstractParty newLender) {
		return new SecurityBasket(this.basketIpfsHash, newLender, this.borrower,
				this.linearId);
	}

	public SecurityBasket withoutLender() {
		return new SecurityBasket(this.basketIpfsHash,
				NullKeys.INSTANCE.getNULL_PARTY(), this.borrower,
				this.linearId);
	}

	public List<PublicKey> getParticipantKeys() {
		return getParticipants().stream().map(AbstractParty::getOwningKey)
				.collect(Collectors.toList());
	}

	@Override
	public String toString() {
		String lenderString;
		if (this.lender instanceof Party) {
			lenderString = ((Party) lender).getName().getOrganisation();
		}
		else {
			PublicKey lenderKey = this.lender.getOwningKey();
			lenderString = toBase58String(lenderKey);
		}

		String borrowerString;
		if (this.borrower instanceof Party) {
			borrowerString = ((Party) borrower).getName().getOrganisation();
		}
		else {
			PublicKey borrowerKey = this.borrower.getOwningKey();
			borrowerString = toBase58String(borrowerKey);
		}

		return String
				.format("Security Basket(%s): %s :: %s has got Security Basket with Key:: %s.",
						this.linearId, borrowerString, lenderString,
						this.basketIpfsHash);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SecurityBasket)) {
			return false;
		}
		SecurityBasket other = (SecurityBasket) obj;
		return basketIpfsHash.equals(other.getBasketIpfsHash())
				&& lender.equals(other.getLender())
				&& borrower.equals(other.getBorrower())
				&& linearId.equals(other.getLinearId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(basketIpfsHash, lender, borrower, linearId);
	}
}
