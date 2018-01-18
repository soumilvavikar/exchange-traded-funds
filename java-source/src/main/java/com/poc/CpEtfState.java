package com.poc;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Currency;
import java.util.List;

public class CpEtfState  implements OwnableState {
    private String etfCode;
    private AbstractParty owner;
    private String etfName;
    private int quantity;


    public CpEtfState() {
    }  // For serialization

    public CpEtfState( String etfCode, String etfName, int quantity, AbstractParty owner) {

        this.owner = owner;
        this.etfCode = etfCode;
        this.etfName = etfName;
        this.quantity = quantity;
    }

    public CpEtfState copy() {
        return new CpEtfState(this.etfCode, this.etfName, this.quantity, this.owner);
    }

    public CpEtfState withoutOwner() {
        return new CpEtfState(this.etfCode, this.etfName, this.quantity, new AnonymousParty(NullKeys.NullPublicKey.INSTANCE));
    }

    @NotNull
    @Override
    public CommandAndState withNewOwner(@NotNull AbstractParty newOwner) {
        return new CommandAndState(new CpEtf.Commands.Move(), new CpEtfState(this.etfCode, this.etfName, this.quantity, newOwner));
    }

    public AbstractParty getOwner() {
        return owner;
    }

    public String getEtfCode() {
        return etfCode;
    }

    public String getEtfName() {
        return etfName;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CpEtfState state = (CpEtfState) o;


        if (owner != null ? !owner.equals(state.owner) : state.owner != null) {
            return false;
        }
        if (etfCode != null ? !etfCode.equals(state.etfCode) : state.etfCode != null){
            return false;
        }

        if (etfName != null ? !etfName.equals(state.etfName) : state.etfName != null){
            return false;
        }

        if (quantity != 0 ? quantity != state.quantity : state.quantity != 0){
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (etfCode != null ? etfCode.hashCode() : 0);
        result = 31 * result + (etfName != null ? etfName.hashCode() : 0);
        return result;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(this.owner);
    }
}
