package com.cts.etf.api;

import net.corda.core.contracts.Amount;
import net.corda.core.identity.Party;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.obligation.flows.IssueObligation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Currency;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

public class CreateEtfApi {

    @GET
    @Path("create-etf")
    public Response CreateEtf(
    @QueryParam(value = "basketIpfsHash") String basketIpfsHash,
    @QueryParam(value = "party") String party) {
    {

        // 1. Get party objects for the counterparty.
        final Set<Party> lenderIdentities = rpcOps.partiesFromName(party, false);
        if (lenderIdentities.size() != 1) {
            final String errMsg = String.format("Found %d identities for the lender.", lenderIdentities.size());
            throw new IllegalStateException(errMsg);
        }
        final Party lenderIdentity = lenderIdentities.iterator().next();

        // 2. Create an amount object.
       // final Amount issueAmount = new Amount<>((long) amount * 100, Currency.getInstance(currency));

        // 2. Create an ETF Request object.
         final Amount issueAmount = new Amount<>((long) amount * 100, Currency.getInstance(currency));


        // 3. Start the IssueObligation flow. We block and wait for the flow to return.
        try {
            final FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(
                    IssueObligation.Initiator.class,
                    issueAmount, lenderIdentity, true
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
