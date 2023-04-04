package com.contractreview.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/upload")
public class UploadREST {

    @GET
    public Response upload() {
        return Response.status(Status.OK).entity("OK").build();
    }
    
}
