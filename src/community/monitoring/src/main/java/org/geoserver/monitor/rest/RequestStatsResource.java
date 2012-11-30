/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geoserver.monitor.Filter;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.rest.StatsVisitor.Organizer;
import org.geoserver.monitor.rest.StatsVisitor.RequestProperties;
import org.geoserver.monitor.rest.StatsVisitor.RequestStats;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.ReflectiveResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.ReflectiveHTMLFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.feature.type.DateUtil;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;

import freemarker.template.Configuration;

public class RequestStatsResource extends ReflectiveResource {

    private Monitor monitor;

    public RequestStatsResource(Monitor monitor) {
        this.monitor = monitor;
    }

//    protected RequestStats collectData(Monitor monitor, String[] properties, String[] group,
//            Filter[] filters) {
//        final StatsVisitor g = new StatsVisitor();
//        Query query = new Query();
//        if (properties != null)
//            query.properties(properties);
//        if (group != null)
//            query.group(group);
//        if (filters != null) {
//            for (Filter filter : filters) {
//                query.filter(filter);
//            }
//        }
//
//        monitor.query(query, g);
//        return g.getData();
//    }

    CSVFormat createCSVFormat(Request request, Response response) {
        return new CSVFormat(getFields(request), monitor);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        List<DataFormat> formats = super.createSupportedFormats(request, response);
        formats.add(createCSVFormat(request, response));
        // formats.add(createZIPFormat(request, response));
        // formats.add(createExcelFormat(request, response));
        return formats;
    }

    static class CSVFormat extends StreamDataFormat {

        Monitor monitor;

        String[] fields;

        // Regexp matching problematic characters which trigger quoted text mode.
        static Pattern escapeRequired = Pattern.compile("[\\,\\s\"]");

        protected CSVFormat(String[] fields, Monitor monitor) {
            super(new MediaType("application/csv"));

            this.fields = fields;
            this.monitor = monitor;
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out));

            // write out the header
            StringBuffer sb = new StringBuffer();
            for (String fld : fields) {
                sb.append(fld).append(",");
            }
            sb.setLength(sb.length() - 1);
            w.write(sb.append("\n").toString());

            handleRequests(object, monitor);

            w.flush();
        }

        void writeRequest(RequestData data, BufferedWriter w) throws IOException {
            StringBuffer sb = new StringBuffer();

            for (String fld : fields) {
                Object val = OwsUtils.get(data, fld);
                if (val instanceof Date) {
                    val = DateUtil.serializeDateTime((Date) val);
                }
                if (val != null) {
                    String string = val.toString();
                    Matcher match = escapeRequired.matcher(string);
                    if (match.find()) { // may need escaping, so escape
                        string = string.replaceAll("\"", "\"\"");// Double all double quotes to escape
                        sb.append("\"");
                        sb.append(string);
                        sb.append("\"");
                    } else { // No need for escaping
                        sb.append(string);
                    }
                }
                sb.append(",");
            }
            sb.setLength(sb.length() - 1); // Remove trailing comma
            sb.append("\n");
            w.write(sb.toString());
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return null;
        }

    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new HTMLFormat(request, response, this, monitor);
    }

    String[] getFields(Request request) {
        String fields = getAttribute("fields");

        if (fields != null) {
            return fields.split(";");
        } else {
            List<String> props = OwsUtils.getClassProperties(RequestStats.class).properties();

            props.remove("Class");
            props.remove("Body");
            props.remove("Error");

            return props.toArray(new String[props.size()]);
        }
    }

    static class HTMLFormat extends ReflectiveHTMLFormat {

        Monitor monitor;

        protected HTMLFormat(Request request, Response response, Resource resource, Monitor monitor) {
            super(RequestStats.class, request, response, resource);
            this.monitor = monitor;
        }

        @Override
        public Representation toRepresentation(Object object) {
            if (object instanceof RequestStats) {
                return super.toRepresentation(object);
            }

            // TODO: stream this!
            final List<RequestData> requests = new ArrayList();
            handleRequests(object, monitor);

            return super.toRepresentation(requests);
        }

        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = super.createConfiguration(data, clazz);
            cfg.setClassForTemplateLoading(RequestStats.class, "");
            return cfg;
        }

        @Override
        protected String getTemplateName(Object data) {
            return "stats.html";
        }
    }

    static void handleRequests(Object object, Monitor monitor) {
        // if (object instanceof Query) {
        // monitor.query((Query) object, new StatsVisitor());
        // } else {
        // Map<Object, RequestStats> requests;
        // if (object instanceof Map) {
        // requests = (Map) object;
        // }
        // else {
        // requests = (RequestStats)object);
        // }
        // for (RequestData data : requests) {
        // new StatsVisitor().visit(data, null);
        // }
        // }
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        Map attrs = getRequest().getAttributes();
        Reference ref = getRequest().getResourceRef();
        Form form = ref.getQueryAsForm();
        Query query = new Query();
        
        // ?properties=time&aggregate=remoteLon
        String[] properties = null;
        final List<RequestProperties> requestProperties = new ArrayList<StatsVisitor.RequestProperties>();
        String props = form.getValues("properties", ",", true);
        if (props != null) {
            properties = props.split(",");
            query.properties(properties);
            for (String prop : properties) {
                RequestProperties p = RequestProperties.valueOf(prop);
                if (p != null) {
                    requestProperties.add(p);
                } else {
                    throw new IllegalArgumentException("Unable to parse property: "+prop
                            +"\nAvailables are: "+RequestProperties.values());
                }
            }
        }

        String[] group = null;
        String grp = form.getValues("group", ",", true);
        if (grp != null) {
            group = grp.split(",");
            final StatsVisitor g = new StatsVisitor(new Organizer() {
                @Override
                public String groupBy(RequestData req) {
                    return parseGroup(group);
                }

                @Override
                public RequestProperties[] useProperties() {
                    // if properties filter is set
                    if (!requestProperties.isEmpty())
                        return requestProperties.toArray(new RequestProperties[] {});
                    // else all the properties are requested
                    return RequestProperties.values();
                }
            });
            monitor.query(query, g);
            g.getData();
        }

        String[] filters = null;
        String fs = form.getValues("filters",",",true);
        if (fs != null) {
             filters=fs.split(",");
             for (String filter : filters){
                 Filter f=parseFilter(filter);
                 if (f!=null){
                     query.filter(f);
                 } else {
                     throw new IllegalArgumentException("Unable to parse filter: "+f);
                 }
             }
        }

    }
    
    private static String parseGroup(String group, RequestData req){
        // TODO parse and combine request properties using group string
        // f.e.:
        // group = "path+host"
        // return req.getPath()+req.getHost();
        
        return req.getPath();
    }
    private static Filter parseFilter(String filter){
        //TODO filter parser
        return new Filter("service", "WMS", Comparison.EQ);
    }
}
