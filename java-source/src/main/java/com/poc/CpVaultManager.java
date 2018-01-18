package com.poc;

import com.cts.etf.SecurityBasket;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CpVaultManager {
    private VaultService vaultService;

    public CpVaultManager(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    public StateAndRef<State> getSecurityBasket(String basketIpfsHash) {

        Set<Class<State>> stateSet = new HashSet<>();
        stateSet.add(State.class);
        EnumSet<Vault.StateStatus> stateStatus = EnumSet.of(Vault.StateStatus.UNCONSUMED);

//        Iterable<StateAndRef<SecurityBasket>> vaultStates = vaultService.queryBy(SecurityBasket.class);
        Vault.Page<State> result = vaultService.queryBy(State.class);
        Iterable<StateAndRef<State>> vaultStates = result.getStates();

        StateAndRef<State> inputStateAndRef = null;

        Iterator<StateAndRef<State>> it;
        it = vaultStates.iterator();
        while (it.hasNext()) {
            StateAndRef<State> stateAndRef = it.next();
            if (stateAndRef.getState().getData().getBasketIpfsHash().equals(basketIpfsHash)) {
                inputStateAndRef = stateAndRef;
                break;
            }
        }
        return inputStateAndRef;
    }

    public StateAndRef<CpEtfState> getEtfs(String etfCode) {

        Set<Class<CpEtfState>> stateSet = new HashSet<>();
        stateSet.add(CpEtfState.class);
        EnumSet<Vault.StateStatus> stateStatus = EnumSet.of(Vault.StateStatus.UNCONSUMED);

//        Iterable<StateAndRef<SecurityBasket>> vaultStates = vaultService.queryBy(SecurityBasket.class);
        Vault.Page<CpEtfState> result = vaultService.queryBy(CpEtfState.class);
        Iterable<StateAndRef<CpEtfState>> vaultStates = result.getStates();

        StateAndRef<CpEtfState> inputStateAndRef = null;

        Iterator<StateAndRef<CpEtfState>> it;
        it = vaultStates.iterator();
        while (it.hasNext()) {
            StateAndRef<CpEtfState> stateAndRef = it.next();
            if (stateAndRef.getState().getData().getEtfCode().equals(etfCode)) {
                inputStateAndRef = stateAndRef;
                break;
            }
        }
        return inputStateAndRef;
    }
}
