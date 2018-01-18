package com.poc;

import com.cts.common.ApplicationPlugin;
import com.cts.etf.SecurityBasket;
import com.cts.etf.contracts.SecurityBasketContract;
import com.cts.etf.flows.IouSecurityBasketFlow;
import com.cts.etf.flows.IouSecurityBasketSettleFlow;
import com.cts.etf.flows.IssueSecurityBasketFlow;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.ServiceHub;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.examples.obligation.Obligation;
import net.corda.examples.obligation.flows.IssueObligation;
import net.corda.examples.obligation.flows.SettleObligation;
import net.corda.finance.flows.AbstractCashFlow;
import net.corda.finance.flows.CashIssueFlow;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

@Path("cp-basket")
public class CpBasketApi implements ApplicationPlugin {

    private final CordaRPCOps rpcOps;
    private final Party myIdentity;

    public CpBasketApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myIdentity = rpcOps.nodeInfo().getLegalIdentities().get(0);
    }

    @GET
    @Path("get")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<State>> cpBaskets() {
        return rpcOps.vaultQuery(State.class).getStates();
    }

    @GET
    @Path("issue")
    public Response issueCpBasket(
            @QueryParam(value = "basketIpfsHash") String basketIpfsHash )
    {
        final Party notary = rpcOps.notaryIdentities().get(0);

        try {
            final FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(
                    CpBasketIssueFlow.Initiator.class,
                    basketIpfsHash, myIdentity, notary, false
            );

            final SignedTransaction result = flowHandle.getReturnValue().get();
            final String msg = String.format("Basket with hash id %s committed to ledger.\n%s",
                    result.getId(), result.getTx().getOutputStates().get(0));
            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("move")
    public Response moveCpBasket(
            @QueryParam(value = "basketIpfsHash") String basketIpfsHash,
            @QueryParam(value = "party") String party) {
        // 1. Get party objects for the counterparty.
        final Set<Party> newOwners = rpcOps.partiesFromName(party, false);
        if (newOwners.size() != 1) {
            final String errMsg = String.format("Found %d identities for the lender.", newOwners.size());
            throw new IllegalStateException(errMsg);
        }
        final Party newOwner = newOwners.iterator().next();

        final Party notary = rpcOps.notaryIdentities().get(0);

        // 2. Create an amount object.
        // final Amount issueAmount = new Amount<>((long) amount * 100, Currency.getInstance(currency));

        // 3. Start the IssueObligation flow. We block and wait for the flow to return.
        try {
            final FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(
                    CpBasketMoveFlow.Initiator.class,
                    basketIpfsHash, this.myIdentity, newOwner, notary, false
            );

            final SignedTransaction result = flowHandle.getReturnValue().get();
            final String msg = String.format("Transaction id %s committed to ledger.\n%s",
                    result.getId(), result.getTx().getOutputStates().get(0));
            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("settle-iou")
    public Response settleObligation(
            @QueryParam(value = "id") String id,
            @QueryParam(value = "basketIpfsHash") String basketIpfsHash) {
        UniqueIdentifier linearId = UniqueIdentifier.Companion.fromString(id);
        // Amount<Currency> settleAmount = new Amount<>((long) amount * 100, Currency.getInstance(currency));

        try {
            final FlowHandle flowHandle = rpcOps.startFlowDynamic(
                    IouSecurityBasketSettleFlow.Initiator.class,
                    linearId, basketIpfsHash, true);

            flowHandle.getReturnValue().get();
            final String msg = String.format("%s paid off on Security Basket IOU id %s.", basketIpfsHash, id);
            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}
