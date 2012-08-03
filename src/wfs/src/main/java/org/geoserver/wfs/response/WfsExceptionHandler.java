/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONException;

import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.DefaultServiceExceptionHandler;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.json.JSONType;

import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * Handles a wfs service exception by producing an exception report.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class WfsExceptionHandler extends DefaultServiceExceptionHandler {

    GeoServer gs;
    
    /**
     * @param service The wfs service descriptors.
     */
    public WfsExceptionHandler(List services, GeoServer gs) {
        super(services);
        this.gs = gs;
    }

    public WFSInfo getInfo() {
        return gs.getService(WFSInfo.class);
    }
    
    /**
     * Encodes a ogc:ServiceExceptionReport to output.
     */
    public void handleServiceException(ServiceException exception, Request request) {

        boolean verbose=gs.getSettings().isVerboseExceptions();
    	String charset=gs.getSettings().getCharset();
    	// first of all check what kind of exception handling we must perform
        final String exceptions;
        try {
            exceptions = (String) request.getKvp().get("EXCEPTIONS");
            if (exceptions == null) {
            	// use default
            	handleDefault(exception, request, charset, verbose);
                return;
            }
        } catch (Exception e) {
            // width and height might be missing
            handleDefault(exception, request, charset, verbose);
            return;
        }
        if (JSONType.isJsonMimeType(exceptions)){
        	// use Json format
        	handleJsonException(exception, request, charset, verbose, false);
        } else if (JSONType.isJsonpMimeType(exceptions)){
        	// use JsonP format
        	handleJsonException(exception, request, charset, verbose, true);
        } else {
        	handleDefault(exception,request,charset, verbose);
        }
        	
    }
    
    private void handleDefault(ServiceException exception, Request request, String charset, boolean verbose){
    	if ("1.0.0".equals(request.getVersion())) {
            handle1_0(exception, request.getHttpResponse());
        } else {
            super.handleServiceException(exception, request);
        }
    }
    

    private void handleJsonException(ServiceException exception, Request request, String charset, boolean verbose, boolean isJsonp) {
    	
    	final HttpServletResponse response = request.getHttpResponse();
    	response.setContentType(JSONType.jsonp);
        // TODO: server encoding options?
        response.setCharacterEncoding(charset);
        
        ServletOutputStream os = null;
    	try {
    		os=response.getOutputStream();
    		if (isJsonp) {
    			// jsonp
    			JSONType.writeJsonpException(exception,request,os,charset,verbose);
    		} else {
    			// json
    			OutputStreamWriter outWriter = null;
    			try {
    				outWriter = new OutputStreamWriter(os, charset);
    				JSONType.writeJsonException(exception, request, outWriter, verbose);
    			} finally {
    				if (outWriter != null) {
    	    			try {
    	    				outWriter.flush();
    	    			} catch (IOException ioe){}
    					IOUtils.closeQuietly(outWriter);
    				}
    			}

    		}
    	} catch (Exception e){
    		LOGGER.warning(e.getLocalizedMessage());
    	} finally {
    		if (os!=null){
    			try {
    				os.flush();
    			} catch (IOException ioe){}
    			IOUtils.closeQuietly(os);
    		}
    	}
    }

    public void handle1_0(ServiceException e, HttpServletResponse response) {
        try {
            String tab = "   ";

            StringBuffer s = new StringBuffer();
            s.append("<?xml version=\"1.0\" ?>\n");
            s.append("<ServiceExceptionReport\n");
            s.append(tab + "version=\"1.2.0\"\n");
            s.append(tab + "xmlns=\"http://www.opengis.net/ogc\"\n");
            s.append(tab + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            s.append(tab);
            s.append("xsi:schemaLocation=\"http://www.opengis.net/ogc ");
            s.append(ResponseUtils.appendPath(getInfo().getSchemaBaseURL(), "wfs/1.0.0/OGC-exception.xsd")
                + "\">\n");

            s.append(tab + "<ServiceException");

            if ((e.getCode() != null) && !e.getCode().equals("")) {
                s.append(" code=\"" + e.getCode() + "\"");
            }

            if ((e.getLocator() != null) && !e.getLocator().equals("")) {
                s.append(" locator=\"" + e.getLocator() + "\"");
            }

            s.append(">");

            if (e.getMessage() != null) {
                s.append("\n" + tab + tab);
                OwsUtils.dumpExceptionMessages(e, s, true);

                if(verboseExceptions) {
                  ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
                  e.printStackTrace(new PrintStream(stackTrace));

                  s.append("\nDetails:\n");
                  s.append(ResponseUtils.encodeXML(new String(stackTrace.toByteArray())));
                }
            }

            s.append("\n</ServiceException>");
            s.append("</ServiceExceptionReport>");

            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            response.getOutputStream().write(s.toString().getBytes());
            response.getOutputStream().flush();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
