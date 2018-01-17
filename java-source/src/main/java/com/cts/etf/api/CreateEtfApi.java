package com.cts.etf.api;

import com.cts.etf.CreateEtfRequest;
import com.cts.etf.flows.EtfCreationRequestFlow;
import net.corda.core.contracts.StateAndRef;
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

@Path("etf-request")
public class CreateEtfApi {

    private final CordaRPCOps rpcOps;
    private final Party myIdentity;

    public CreateEtfApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myIdentity = rpcOps.nodeInfo().getLegalIdentities().get(0);
    }
    @GET
    @Path("create")
    public Response CreateEtf(
    @QueryParam(value = "basketIpfsHash") String basketIpfsHash,
    @QueryParam(value = "sponserer") String sponserer,
    @QueryParam(value = "etfCode") String etfCode)
    {

        // 1. Get party objects for the counterparty.
        final Set<Party> borrowerIdentities = rpcOps.partiesFromName(sponserer, false);
        if (borrowerIdentities.size() != 1) {
            final String errMsg = String.format("Found %d identities for the lender.", borrowerIdentities.size());
            throw new IllegalStateException(errMsg);
        }
        final Party borrowerIdentity = borrowerIdentities.iterator().next();

        // 2. Create an amount object.
       // final Amount issueAmount = new Amount<>((long) amount * 100, Currency.getInstance(currency));

        // 2. Create an ETF Request object.
       //  final Amount issueAmount = new Amount<>((long) amount * 100, Currency.getInstance(currency));


        // 3. Start the Issue Obligation flow. We block and wait for the flow to return.
        try {
            final FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(
                    EtfCreationRequestFlow.Initiator.class,
                    basketIpfsHash,etfCode, borrowerIdentity, false
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
    @Path("get")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<CreateEtfRequest>> securityBaskets() {
        return rpcOps.vaultQuery(CreateEtfRequest.class).getStates();
    }
}
