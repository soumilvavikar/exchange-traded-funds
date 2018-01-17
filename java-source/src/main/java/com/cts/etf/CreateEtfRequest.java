package com.cts.etf;

import com.cts.etf.api.EtfApi;
import com.cts.etf.contracts.CreateEtfRequestContract;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.finance.contracts.DealState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static net.corda.core.utilities.EncodingUtils.toBase58String;

@CordaSerializable
public class CreateEtfRequest implements DealState {

    private static final Logger logger =  Logger.getLogger(CreateEtfRequest.class.toString());

    private final String basketIpfsHash;
    private final AbstractParty lender; // AP (Authorized Paarticipant)
    private final AbstractParty borrower; // This can be ETF Sponsorer
    private final String etfCode;
    private final int quantity;
    private final UniqueIdentifier linearId;


    public CreateEtfRequest(String basketIpfsHash, String etfCode, AbstractParty lender, AbstractParty borrower, int quantity, UniqueIdentifier linearId) {
        this.basketIpfsHash = basketIpfsHash;
        this.etfCode = etfCode;
        this.lender = lender;
        this.borrower = borrower;
        this.quantity = quantity;
        this.linearId = linearId;

    }

    public CreateEtfRequest(String basketIpfsHash, String etfCode, AbstractParty lender, AbstractParty borrower, int quantity) {
        this.basketIpfsHash = basketIpfsHash;
        this.etfCode = etfCode;
        this.lender = lender;
        this.borrower = borrower;
        this.quantity = quantity;
        this.linearId = new UniqueIdentifier();
    }

    public String getBasketIpfsHash() {
        return basketIpfsHash;
    }

    public String getEtfCode() {
        return etfCode;
    }

    public AbstractParty getLender() {
        return lender;
    }

    public AbstractParty getBorrower() {
        return borrower;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(lender, borrower);
    }

    public SecurityBasket withNewLender(AbstractParty newLender) {
        return new SecurityBasket(this.basketIpfsHash, newLender, this.borrower, this.linearId);
    }

    public SecurityBasket withoutLender() {
        return new SecurityBasket(this.basketIpfsHash, NullKeys.INSTANCE.getNULL_PARTY(), this.borrower, this.linearId);
    }

    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        String lenderString;
        if (this.lender instanceof Party) {
            lenderString = ((Party) lender).getName().getOrganisation();
        } else {
            PublicKey lenderKey = this.lender.getOwningKey();
            lenderString = toBase58String(lenderKey);
        }

        String borrowerString;
        if (this.borrower instanceof Party) {
            borrowerString = ((Party) borrower).getName().getOrganisation();
        } else {
            PublicKey borrowerKey = this.borrower.getOwningKey();
            borrowerString = toBase58String(borrowerKey);
        }

        return String.format("ETF Creation Request (%s): has been raised to %s :: by %s against Security Basket:: %s.",
                this.linearId, borrowerString, lenderString, this.basketIpfsHash);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SecurityBasket)) {
            return false;
        }
        SecurityBasket other = (SecurityBasket) obj;
        return basketIpfsHash.equals(other.getBasketIpfsHash())
                && lender.equals(other.getLender())
                && borrower.equals(other.getBorrower())
                && linearId.equals(other.getLinearId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(basketIpfsHash, lender, borrower, linearId);
    }

    @NotNull
    @Override
    public TransactionBuilder generateAgreement(Party notary) {
        System.out.println("In generateAgreement.... ");
        logger.info("CreateEtfRequest.generateAgreement() call Start");
        final TransactionBuilder utx = new TransactionBuilder(notary).withItems(
                new StateAndContract(this, CreateEtfRequestContract.CREATE_ETF_REQUEST_CONTRACT_ID),
                new Command(new CreateEtfRequestContract.Commands.Issue(), getParticipantKeys()));
                /*
                .addOutputState(createEtfRequest, CreateEtfRequestContract.CREATE_ETF_REQUEST_CONTRACT_ID)
                .addCommand(new CreateEtfRequestContract.Commands.Issue(), requiredSigners);
*/
        System.out.println("In generateAgreement 213.... ");
        logger.info("CreateEtfRequest.generateAgreement() called - " + utx.toString());
        return utx;
    }
}
