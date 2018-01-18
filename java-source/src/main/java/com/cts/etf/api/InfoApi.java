package com.cts.etf.api;

import com.cts.common.ApplicationPlugin;
import com.google.common.collect.ImmutableMap;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Path("info")
public class InfoApi implements ApplicationPlugin {

	private final CordaRPCOps rpcOps;
	private final Party myIdentity;

	public InfoApi(CordaRPCOps rpcOps) {
		this.rpcOps = rpcOps;
		this.myIdentity = rpcOps.nodeInfo().getLegalIdentities().get(0);
	}

	@GET
	@Path("me")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Party> me() {
		return ImmutableMap.of("data", myIdentity);
	}

	@GET
	@Path("peers")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, List<String>> peers() {
		return ImmutableMap.of("data", rpcOps.networkMapSnapshot()
				.stream()
				.filter(nodeInfo -> nodeInfo.getLegalIdentities().get(0) != myIdentity)
				.map(it -> it.getLegalIdentities().get(0).getName().getOrganisation())
				.collect(toList()));
	}
}
