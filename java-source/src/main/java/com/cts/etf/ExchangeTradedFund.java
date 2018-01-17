package com.cts.etf;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@CordaSerializable
public class ExchangeTradedFund implements LinearState {
	private final String etfCode;
	private final AbstractParty owner;
	private final AbstractParty buyer;
	private final UniqueIdentifier linearId;
	private final String etfName;
	private final int quantity;
	private final Amount<Currency> faceValue =
			new Amount<>((long) 100, Currency.getInstance("USD"));

	public ExchangeTradedFund(String etfCode,
			AbstractParty owner,
			AbstractParty buyer,
			UniqueIdentifier linearId, String etfName, int quantity) {
		this.etfCode = etfCode;
		this.owner = owner;
		this.buyer = buyer;

		this.linearId = linearId;
		this.etfName = etfName;
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "ExchangeTradedFund{" +
				"etfCode='" + etfCode + '\'' +
				", owner=" + owner +
				", buyer=" + buyer +
				", linearId=" + linearId +
				", etfName='" + etfName + '\'' +
				", quantity=" + quantity +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ExchangeTradedFund that = (ExchangeTradedFund) o;
		return quantity == that.quantity &&
				Objects.equals(etfCode, that.etfCode) &&
				Objects.equals(owner, that.owner) &&
				Objects.equals(buyer, that.buyer) &&
				Objects.equals(linearId, that.linearId) &&
				Objects.equals(etfName, that.etfName);
	}

	@Override
	public int hashCode() {

		return Objects.hash(etfCode, owner, buyer, linearId, etfName, quantity);
	}

	public ExchangeTradedFund(String etfCode,
			AbstractParty owner,
			AbstractParty buyer,
			String etfName,
			int quantity) {
		this.etfCode = etfCode;
		this.owner = owner;
		this.buyer = buyer;
		this.etfName = etfName;
		this.quantity = quantity;

		this.linearId = new UniqueIdentifier();
	}

	public String getEtfCode() {
		return etfCode;
	}

	public AbstractParty getOwner() {
		return owner;
	}

	public AbstractParty getBuyer() {
		return buyer;
	}

	public String getEtfName() {
		return etfName;
	}

	public int getQuantity() {
		return quantity;
	}

	public Amount<Currency> getFaceValue() {
		return faceValue;
	}

	@NotNull
	@Override
	public UniqueIdentifier getLinearId() {
		return linearId;
	}

	@NotNull
	@Override
	public List<AbstractParty> getParticipants() {
		return ImmutableList.of(owner, buyer);
	}

	public List<PublicKey> getParticipantKeys() {
		return getParticipants().stream().map(AbstractParty::getOwningKey)
				.collect(
						Collectors.toList());
	}
}
