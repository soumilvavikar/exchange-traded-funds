package com.cts.etf.flows;

import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.obligation.flows.IssueObligation;

public class IssueSecurityBasketFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends EtfBaseFlow {

    }

    @InitiatedBy(IssueSecurityBasketFlow.Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

    }
}
