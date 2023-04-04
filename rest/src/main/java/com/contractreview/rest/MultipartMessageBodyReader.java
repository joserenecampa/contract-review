package com.contractreview.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;

import org.apache.commons.fileupload.MultipartStream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

@Provider
@Consumes({MediaType.MULTIPART_FORM_DATA})
public class MultipartMessageBodyReader implements MessageBodyReader<ContractDocument> {

    @Override
    public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public ContractDocument readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        Iterator<String> keys = httpHeaders.keySet().iterator();
        String boundaryString = null;
        Integer contentLength = 0;
        while(keys.hasNext()){
            String theKey = (String)keys.next();
            if (theKey.equalsIgnoreCase("content-type")) {
                String value = (String)httpHeaders.getFirst(theKey);
                boundaryString = value.split("boundary=")[1];
            }
            if (theKey.equalsIgnoreCase("content-length")) {
                String value = (String)httpHeaders.getFirst(theKey);
                contentLength = Integer.parseInt(value);
            }
        }
        byte[] boundary = boundaryString.getBytes();
        MultipartStream multipartStream = new MultipartStream(entityStream, boundary, contentLength, null);
        boolean nextPart = multipartStream.skipPreamble();
        ContractDocument cd = new ContractDocument();
        while (nextPart) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
            multipartStream.readBodyData(baos);
            cd.setContent(baos.toByteArray());
            nextPart = multipartStream.readBoundary();
        }
        return cd;
    }
    
}
