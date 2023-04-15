package com.contractreview.rest;

import java.io.IOException;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/analysis") @Consumes @Produces
public class UploadREST {

    @EJB
    ParseContract parse;

    @POST
    @Path("/convert-pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String convertPDFToText(ContractDocument contract, @HeaderParam("similaridade") boolean similaridade) throws IOException {
        String result = parse.parse(contract.getContent(), similaridade);
        return result;
    }

    @POST
    @Path("/convert-to-fine-tune")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String convertToFineTune(ContractDocument contract, @HeaderParam("similaridade") boolean similaridade) throws IOException {
        String result = parse.parse(contract.getContent(), similaridade);
        return result;
    }

}
