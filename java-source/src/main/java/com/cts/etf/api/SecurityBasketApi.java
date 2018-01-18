package com.cts.etf.api;

import com.cts.common.ApplicationPlugin;
import com.cts.etf.SecurityBasket;
import com.cts.etf.flows.IouSecurityBasketFlow;
import com.cts.etf.flows.IouSecurityBasketSettleFlow;
import com.cts.etf.flows.IssueSecurityBasketFlow;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.transactions.SignedTransaction;
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
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

@Path("security-basket")
public class SecurityBasketApi implements ApplicationPlugin {

	private final CordaRPCOps rpcOps;
	private final Party myIdentity;

	public SecurityBasketApi(CordaRPCOps rpcOps) {
		this.rpcOps = rpcOps;
		this.myIdentity = rpcOps.nodeInfo().getLegalIdentities().get(0);
	}

	@GET
	@Path("get")
	@Produces(MediaType.APPLICATION_JSON)
	public List<StateAndRef<SecurityBasket>> securityBaskets() {
		return rpcOps.vaultQuery(SecurityBasket.class).getStates();
	}

	@GET
	@Path("issue")
	public Response issueSecurityBasket(
			@QueryParam(value = "basketIpfsHash") String basketIpfsHash,
			@QueryParam(value = "party") String party) {

		// 1. Get party objects for the counterparty.
		final Set<Party> lenderIdentities =
				rpcOps.partiesFromName(party, false);
		if (lenderIdentities.size() != 1) {
			final String errMsg =
					String.format("Found %d identities for the lender.",
							lenderIdentities.size());
			throw new IllegalStateException(errMsg);
		}
		final Party lenderIdentity = lenderIdentities.iterator().next();

		// 2. Create an amount object.
		selfIssue();

		// 3. Start the IssueObligation flow. We block and wait for the flow to return.
		try {
			final FlowHandle<SignedTransaction> flowHandle =
					rpcOps.startFlowDynamic(
							IssueSecurityBasketFlow.Initiator.class,
							basketIpfsHash, myIdentity, false
					);

			final SignedTransaction result = flowHandle.getReturnValue().get();
			final String msg =
					String.format("Transaction id %s committed to ledger.\n%s",
							result.getId(),
							result.getTx().getOutputStates().get(0));
			return Response.status(CREATED).entity(msg).build();
		}
		catch (Exception e) {
			return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("iou")
	public Response iouSecurityBasket(
			@QueryParam(value = "basketIpfsHash") String basketIpfsHash,
			@QueryParam(value = "party") String party) {
		// 1. Get party objects for the counterparty.
		final Set<Party> lenderIdentities =
				rpcOps.partiesFromName(party, false);
		if (lenderIdentities.size() != 1) {
			final String errMsg =
					String.format("Found %d identities for the lender.",
							lenderIdentities.size());
			throw new IllegalStateException(errMsg);
		}
		final Party lenderIdentity = lenderIdentities.iterator().next();

		// 2. Create an amount object.
		// final Amount issueAmount = new Amount<>((long) amount * 100, Currency.getInstance(currency));

		// 3. Start the IssueObligation flow. We block and wait for the flow to return.
		try {
			final FlowHandle<SignedTransaction> flowHandle =
					rpcOps.startFlowDynamic(
							IouSecurityBasketFlow.Initiator.class,
							basketIpfsHash, lenderIdentity, false
					);

			final SignedTransaction result = flowHandle.getReturnValue().get();
			final String msg =
					String.format("Transaction id %s committed to ledger.\n%s",
							result.getId(),
							result.getTx().getOutputStates().get(0));
			return Response.status(CREATED).entity(msg).build();
		}
		catch (Exception e) {
			return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("settle-iou")
	public Response settleSecurityBasketIou(
			@QueryParam(value = "id") String id,
			@QueryParam(value = "basketIpfsHash") String basketIpfsHash) {
		UniqueIdentifier linearId = UniqueIdentifier.Companion.fromString(id);

		try {
			final FlowHandle flowHandle = rpcOps.startFlowDynamic(
					IouSecurityBasketSettleFlow.Initiator.class,
					linearId, basketIpfsHash, true);

			flowHandle.getReturnValue().get();
			final String msg =
					String.format("%s paid off on Security Basket IOU id %s.",
							basketIpfsHash, id);
			return Response.status(CREATED).entity(msg).build();
		}
		catch (Exception e) {
			return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	private void selfIssue() {
		// 1. Prepare issue request.
		final Amount<Currency> issueAmount =
				new Amount<>((long) 100 * 1, Currency.getInstance("INR"));
		final List<Party> notaries = rpcOps.notaryIdentities();
		if (notaries.isEmpty()) {
			throw new IllegalStateException("Could not find a notary.");
		}
		final Party notary = notaries.get(0);
		final OpaqueBytes issueRef = OpaqueBytes.of(new byte[1]);
		final CashIssueFlow.IssueRequest issueRequest =
				new CashIssueFlow.IssueRequest(issueAmount, issueRef, notary);

		// 2. Start flow and wait for response.
		rpcOps.startFlowDynamic(CashIssueFlow.class, issueRequest);
	}
}
