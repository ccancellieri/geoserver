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

import org.apache.commons.io.IOUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
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
	
    
    /**
     * The MIME type of the format this response produces, supported formats are:
     * <ul>
     *  <li>application/json</li>
	 *  <li>text/javascript</li>
     * </ul>
     */
	private enum JSONType {
		JSONP,
		JSON;
		
		public final static String CALLBACK_FUNCTION = "paddingDescribeLayerOutput";
		
		private static String json="application/json";
		private static String jsonp="text/javascript";
		
		public static JSONType getType(String mime){
			if (json.equalsIgnoreCase(mime)){
				return JSON;
			} else if (jsonp.equalsIgnoreCase(mime)){
				return JSONP;
			} else {
				return null; //not valid representation
			}
		}
		
		public String getMimeType(){
			switch (this){
			case JSON:
				return json;
			case JSONP:
				return jsonp;
			default:
				return null;
			}
		}
	}

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
		this.type=JSONType.getType(outputFormat);
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
//			final JSONBuilder jsonWriter = new JSONBuilder(outWriter);
			JsonWriter jsonWriter = new JsonWriter(outWriter);
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
	
	private static void writeLayerDescription(JSONBuilder jsonWriter, LayerDescription layer){
		jsonWriter.value(
					jsonWriter.key("LayerDescription")).array().
								value(jsonWriter.object().key("name").value(layer.getName())).
								value(jsonWriter.object().key("owsURL").value(layer.getOwsURL())).
								value(jsonWriter.object().key("owsType").value(layer.getOwsType())).
							endArray().
							endObject();
	}


	private static String getCallbackFunction() {
		Request request = Dispatcher.REQUEST.get();
		if (request == null) {
			return JSONType.CALLBACK_FUNCTION;
		} else if (!(request.getKvp().get("FORMAT_OPTIONS") instanceof Map)) {
			return JSONType.CALLBACK_FUNCTION;
		}

		Map<String, String> map = (Map<String, String>) request.getKvp().get(
				"FORMAT_OPTIONS");
		String callback = map.get("callback");
		if (callback != null) {
			return callback;
		} else {
			return JSONType.CALLBACK_FUNCTION;
		}
	}

	

}
