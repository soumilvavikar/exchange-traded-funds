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

public class State  implements OwnableState {
    private AbstractParty owner;
    private String basketIpfsHash;

    public State() {
    }  // For serialization

    public State( AbstractParty owner, String basketIpfsHash) {

        this.owner = owner;
        this.basketIpfsHash = basketIpfsHash;
    }

    public State copy() {
        return new State(this.owner, this.basketIpfsHash);
    }

    public State withoutOwner() {
        return new State(new AnonymousParty(NullKeys.NullPublicKey.INSTANCE), this.basketIpfsHash);
    }

    @NotNull
    @Override
    public CommandAndState withNewOwner(@NotNull AbstractParty newOwner) {
        return new CommandAndState(new CpBasket.Commands.Move(), new State(newOwner, this.basketIpfsHash));
    }

    public AbstractParty getOwner() {
        return owner;
    }

    public String getBasketIpfsHash() {
        return basketIpfsHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        State state = (State) o;


        if (owner != null ? !owner.equals(state.owner) : state.owner != null) {
            return false;
        }
        if (basketIpfsHash != null ? !basketIpfsHash.equals(state.basketIpfsHash) : state.basketIpfsHash != null){
            return false;

        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (basketIpfsHash != null ? basketIpfsHash.hashCode() : 0);
        return result;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(this.owner);
    }
}
