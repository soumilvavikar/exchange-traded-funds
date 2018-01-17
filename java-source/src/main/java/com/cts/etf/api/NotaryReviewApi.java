package com.cts.etf.api;

import com.cts.common.ApplicationPlugin;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Path("notary")
public class NotaryReviewApi implements ApplicationPlugin {
	private static final Logger logger =
			Logger.getLogger(EtfApi.class.toString());
	private final CordaRPCOps rpcOps;
	private final Party myIdentity;

	public NotaryReviewApi(CordaRPCOps rpcOps) {
		this.rpcOps = rpcOps;
		this.myIdentity = rpcOps.nodeInfo().getLegalIdentities().get(0);
	}

	@GET
	@Path("get-data")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TransactionState<ContractState>> reviewEtfs() {
		List<SignedTransaction> transactions = rpcOps
				.internalVerifiedTransactionsSnapshot();
		List<TransactionState<ContractState>> transactionsToReturn = new
				ArrayList<>();
		if (null != transactions) {
			for (SignedTransaction t : transactions) {
				transactionsToReturn.addAll(t.getTx().getOutputs());
			}
		}
		return transactionsToReturn;
	}
}
