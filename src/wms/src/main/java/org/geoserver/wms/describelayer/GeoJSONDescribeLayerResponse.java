/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.describelayer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.json.JSONException;
import net.sf.json.util.JSONBuilder;
import net.sf.json.xml.JSONTypes;

import org.apache.commons.io.IOUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.DescribeLayerRequest;
import org.geoserver.wms.WMS;
import org.geotools.data.ows.LayerDescription;
import org.geotools.util.logging.Logging;

import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * A GetFeatureInfo response handler specialized in producing GML 3 data for a
 * GetFeatureInfo request.
 * 
 * <p>
 * This class does not deals directly with GML encoding. Instead, it works by
 * taking the FeatureResults produced in <code>execute()</code> and constructs a
 * <code>GetFeaturesResult</code> wich is passed to a
 * <code>GML2FeatureResponseDelegate</code>, as if it where the result of a
 * GetFeature WFS request.
 * </p>
 * 
 * @author carlo cancellieri
 */
public class GeoJSONDescribeLayerResponse extends DescribeLayerResponse {
	
    
    /** A logger for this class. */
    protected static final Logger LOGGER = Logging.getLogger(GeoJSONDescribeLayerResponse.class);
    
    /**
     * The MIME type of the format this response produces, supported formats see {@link JSONType}
     */
    private final JSONType type;
    

	protected final WMS wms;

	/**
	 * Constructor for subclasses
	 */
	public GeoJSONDescribeLayerResponse(final WMS wms,
			final String outputFormat) {
		super(outputFormat);
		this.wms = wms;
		this.type=JSONType.getJSONType(outputFormat);
		if (type==null)
			throw new IllegalArgumentException("Not supported mime type for:"+outputFormat);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void write(DescribeLayerModel layers,
			DescribeLayerRequest request, OutputStream output)
			throws ServiceException, IOException {

		switch (type) {
		case JSON:
			OutputStreamWriter outWriter = null;
			try {
				outWriter = new OutputStreamWriter(output, wms.getGeoServer()
						.getSettings().getCharset());

				writeJSON(outWriter, layers);
			} finally {

				if (outWriter != null) {
					outWriter.flush();
					IOUtils.closeQuietly(outWriter);
				}
			}
		case JSONP:
			writeJSONP(output, layers);
		}
	}

	private void writeJSONP(OutputStream out, DescribeLayerModel layers)
			throws IOException {
		
		OutputStreamWriter outWriter = null;
		try {
			outWriter = new OutputStreamWriter(out, wms.getGeoServer()
					.getSettings().getCharset());

			outWriter.write(getCallbackFunction() + "(");

			writeJSON(outWriter, layers);
		} finally {

			if (outWriter != null) {
				outWriter.write(")");
				outWriter.flush();
				IOUtils.closeQuietly(outWriter);
			}
		}
	}

	private void writeJSON(OutputStreamWriter outWriter,
			DescribeLayerModel description) throws IOException {

		try {
			final JsonWriter jsonWriter = new JsonWriter(outWriter);
			final List<LayerDescription> layers=description.getLayerDescriptions();
			
			jsonWriter.startNode("WMS_DescribeLayerResponse", String.class);
			jsonWriter.startNode("version", String.class);
				jsonWriter.setValue(description.getVersion());
			jsonWriter.endNode();
			
			for (LayerDescription layer : layers) {
				jsonWriter.startNode("LayerDescription", LayerDescription.class);
					jsonWriter.startNode("name",String.class);
						jsonWriter.setValue(layer.getName());
					jsonWriter.endNode();
					jsonWriter.startNode("owsURL",URL.class);
						jsonWriter.setValue(layer.getOwsURL().toString());
					jsonWriter.endNode();
					jsonWriter.startNode("owsType",String.class);
						jsonWriter.setValue(layer.getOwsType());
					jsonWriter.endNode();
				jsonWriter.endNode();
			}
			jsonWriter.endNode();

		} catch (JSONException jsonException) {
			ServiceException serviceException = new ServiceException("Error: "
					+ jsonException.getMessage());
			serviceException.initCause(jsonException);
			throw serviceException;
		}
	}

	private static String getCallbackFunction() {
		Request request = Dispatcher.REQUEST.get();
		if (request == null) {
			return JSONType.CALLBACK_FUNCTION;
		} else {
			return JSONType.getCallbackFunction(request.getKvp());
		}
	}

	

}
