package com.poc;

import com.cts.common.ApplicationPlugin;
import com.cts.etf.flows.IouSecurityBasketSettleFlow;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.transactions.SignedTransaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

@Path("cp-etf")
public class CpEtfApi  implements ApplicationPlugin {

    private final CordaRPCOps rpcOps;
    private final Party myIdentity;

    public CpEtfApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myIdentity = rpcOps.nodeInfo().getLegalIdentities().get(0);
    }

    @GET
    @Path("get")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<CpEtfState>> cpBaskets() {
        return rpcOps.vaultQuery(CpEtfState.class).getStates();
    }

    @GET
    @Path("issue")
    public Response issueCpEtf(
            @QueryParam(value = "etfName") String etfName,
            @QueryParam(value = "etfCode") String etfCode,
            @QueryParam(value = "quantity") int quantity)
    {
        final Party notary = rpcOps.notaryIdentities().get(0);

        try {
            final FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(
                    CpEtfIssueFlow.Initiator.class,
                    etfCode, etfName, quantity, myIdentity, notary, false
            );

            final SignedTransaction result = flowHandle.getReturnValue().get();
            final String msg = String.format("ETF with hash id %s committed to ledger.\n%s",
                    result.getId(), result.getTx().getOutputStates().get(0));
            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("move")
    public Response moveCpBasket(
            @QueryParam(value = "etfCode") String etfCode,
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
                    CpEtfMoveFlow.Initiator.class,
                    etfCode, this.myIdentity, newOwner, notary, false
            );

            final SignedTransaction result = flowHandle.getReturnValue().get();
            final String msg = String.format("Transaction id %s committed to ledger.\n%s",
                    result.getId(), result.getTx().getOutputStates().get(0));
            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}