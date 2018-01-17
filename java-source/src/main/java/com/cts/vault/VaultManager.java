package com.cts.vault;

import com.cts.etf.ExchangeTradedFund;
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

    public StateAndRef<ExchangeTradedFund> getIssuedEtfs(String etfCode) {

        Set<Class<ExchangeTradedFund>> stateSet = new HashSet<>();
        stateSet.add(ExchangeTradedFund.class);
        EnumSet<Vault.StateStatus> stateStatus = EnumSet.of(Vault.StateStatus.ALL);

//        Iterable<StateAndRef<SecurityBasket>> vaultStates = vaultService.queryBy(SecurityBasket.class);
        Vault.Page<ExchangeTradedFund> result = vaultService.queryBy(ExchangeTradedFund.class);
        Iterable<StateAndRef<ExchangeTradedFund>> vaultStates = result.getStates();

        StateAndRef<ExchangeTradedFund> inputStateAndRef = null;

        Iterator<StateAndRef<ExchangeTradedFund>> it;
        it = vaultStates.iterator();
        while (it.hasNext()) {
            StateAndRef<ExchangeTradedFund> stateAndRef = it.next();
            if (stateAndRef.getState().getData().getEtfCode().equals(etfCode)) {
                inputStateAndRef = stateAndRef;
                break;
            }
        }
        return inputStateAndRef;
    }
}
