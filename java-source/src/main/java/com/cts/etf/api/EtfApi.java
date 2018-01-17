package com.cts.etf.api;

import com.cts.common.ApplicationPlugin;
import com.cts.etf.flows.IssueEtfFlow;
import net.corda.core.contracts.Amount;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.transactions.SignedTransaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Currency;
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
	@Path("issue")
	public Response selfIssueEtf(
			@QueryParam(value = "etfName") String etfName,
			@QueryParam(value = "quantity") int quantity,
			@QueryParam(value = "etfAmount") long amount,
			@QueryParam(value = "currency") String currency) {

		logger.info("Calling the GET API - etf/issue");

		// 2. Create an amount object.
		final Amount etfAmount = new Amount<>((long) amount * 100, Currency
				.getInstance(currency));
		logger.info("etfAmount object created.");

		// 3. Start the IssueEtf flow. We block and wait for the flow to return.
		try {
			logger.info("Calling rpcOps.startFlowDynamic");
			final FlowHandle<SignedTransaction> flowHandle =
					rpcOps.startFlowDynamic(
							IssueEtfFlow.Initiator.class, etfName,
							etfAmount, quantity, this.myIdentity,
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
}
