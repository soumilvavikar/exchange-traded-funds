package com.cts.vault;

import com.cts.etf.SecurityBasket;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class VaultManager {
    
    private VaultService vaultService;

    public VaultManager(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    public StateAndRef<SecurityBasket> getSecurityBasket(String basketIpfsHash) {

        Set<Class<SecurityBasket>> stateSet = new HashSet<>();
        stateSet.add(SecurityBasket.class);
        EnumSet<Vault.StateStatus> stateStatus = EnumSet.of(Vault.StateStatus.UNCONSUMED);

//        Iterable<StateAndRef<SecurityBasket>> vaultStates = vaultService.queryBy(SecurityBasket.class);
        Vault.Page<SecurityBasket> result = vaultService.queryBy(SecurityBasket.class);
        Iterable<StateAndRef<SecurityBasket>> vaultStates = result.getStates();

        StateAndRef<SecurityBasket> inputStateAndRef = null;

        Iterator<StateAndRef<SecurityBasket>> it;
        it = vaultStates.iterator();
        while (it.hasNext()) {
            StateAndRef<SecurityBasket> stateAndRef = it.next();
            if (stateAndRef.getState().getData().getBasketIpfsHash().equals(basketIpfsHash)) {
                inputStateAndRef = stateAndRef;
                break;
            }
        }
        return inputStateAndRef;
    }
}
