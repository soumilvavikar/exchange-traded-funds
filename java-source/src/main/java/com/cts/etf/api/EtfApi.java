package com.cts.etf.api;

import com.cts.common.ApplicationPlugin;
import com.cts.etf.ExchangeTradedFund;
import com.cts.etf.flows.IouEtfFlow;
import com.cts.etf.flows.IouEtfSettleFlow;
import com.cts.etf.flows.IouSecurityBasketSettleFlow;
import com.cts.etf.flows.IssueEtfFlow;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.flows.CashIssueFlow;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

@Path("etf")
public class EtfApi implements ApplicationPlugin {
	private static final Logger logger =
			Logger.getLogger(EtfApi.class.toString());
	private final CordaRPCOps rpcOps;
	private final Party myIdentity;

	public EtfApi(CordaRPCOps rpcOps) {
		this.rpcOps = rpcOps;
		this.myIdentity = rpcOps.nodeInfo().getLegalIdentities().get(0);
	}

	@GET
	@Path("get")
	@Produces(MediaType.APPLICATION_JSON)
	public List<StateAndRef<ExchangeTradedFund>> getEtfs() {
		return rpcOps.vaultQuery(ExchangeTradedFund.class).getStates();
	}

	@GET
	@Path("issue")
	public Response selfIssueEtf(
			@QueryParam(value = "etfName") String etfName,
			@QueryParam(value = "quantity") int quantity,
			@QueryParam(value = "etfCode") String etfCode) {

		logger.info("Calling the GET API - etf/issue");

		try {
			logger.info("Calling rpcOps.startFlowDynamic");
			final FlowHandle<SignedTransaction> flowHandle =
					rpcOps.startFlowDynamic(
							IssueEtfFlow.Initiator.class, etfName,
							etfCode, quantity, this.myIdentity,
							this.myIdentity,
							false
					);
			logger.info("Executed rpcOps.startFlowDynamic");
			selfIssue();

			final SignedTransaction result = flowHandle.getReturnValue().get();
			final String msg =
					String.format("Transaction id %s committed to ledger.\n%s",
							result.getId(),
							result.getTx().getOutputStates().get(0));
			logger.info("Sending response: " + msg);

			return Response.status(CREATED).entity(msg).build();
		}
		catch (Exception e) {
			return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("iou")
	public Response iouEtf(
			@QueryParam(value = "etfName") String etfName,
			@QueryParam(value = "quantity") int quantity,
			@QueryParam(value = "etfCode") String etfCode,
			@QueryParam(value = "party") String party) {

		logger.info("Calling the GET API - etf/iou");
		final Set<Party> lenderIdentities =
				rpcOps.partiesFromName(party, false);
		if (lenderIdentities.size() != 1) {
			final String errMsg =
					String.format("Found %d identities for the lender.",
							lenderIdentities.size());
			throw new IllegalStateException(errMsg);
		}
		final Party lenderIdentity = lenderIdentities.iterator().next();
		try {
			logger.info("Calling rpcOps.startFlowDynamic");
			final FlowHandle<SignedTransaction> flowHandle =
					rpcOps.startFlowDynamic(
							IouEtfFlow.Initiator.class, etfName,
							etfCode, quantity, lenderIdentity,
							this.myIdentity,
							false
					);
			logger.info("Executed rpcOps.startFlowDynamic");

			final SignedTransaction result = flowHandle.getReturnValue().get();
			final String msg =
					String.format("Transaction id %s committed to ledger.\n%s",
							result.getId(),
							result.getTx().getOutputStates().get(0));
			logger.info("Sending response: " + msg);

			return Response.status(CREATED).entity(msg).build();
		}
		catch (Exception e) {
			return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("settle-iou")
	public Response settleEtfIou(
			@QueryParam(value = "etfCode") String etfCode,
			@QueryParam(value = "etfHash") String etfHash) {
		UniqueIdentifier linearId = UniqueIdentifier.Companion.fromString(etfHash);

		try {
			final FlowHandle flowHandle = rpcOps.startFlowDynamic(
					IouEtfSettleFlow.Initiator.class,
					linearId, etfCode, true);

			flowHandle.getReturnValue().get();
			final String msg =
					String.format("ETF IOU settled. EtfId: " +
							etfHash);
			return Response.status(CREATED).entity(msg).build();
		}
		catch (Exception e) {
			return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	private void selfIssue() {
		final Amount<Currency>
				issueAmount = new Amount<>((long) 100, Currency
				.getInstance("USD"));
		final OpaqueBytes issueRef = OpaqueBytes.of(new byte[1]);
		final List<Party> notaries = rpcOps.notaryIdentities();
		if (notaries.isEmpty()) {
			throw new IllegalStateException("Could not find a notary.");
		}

		final Party notary = notaries.get(0);
		final CashIssueFlow.IssueRequest issueRequest =
				new CashIssueFlow.IssueRequest(issueAmount, issueRef,
						notary);
		rpcOps.startFlowDynamic(CashIssueFlow.class, issueRequest);
	}
}
