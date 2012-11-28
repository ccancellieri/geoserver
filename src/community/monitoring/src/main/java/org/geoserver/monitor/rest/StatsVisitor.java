package org.geoserver.monitor.rest;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geotools.math.Statistics;

public class StatsVisitor implements RequestDataVisitor {
    private Map<String, RequestStats> data = new HashMap<String, RequestStats>();

    private final Organizer organizer;

    public StatsVisitor(final Organizer organizer) {
        super();
        this.organizer = organizer;
    }

    public void visit(RequestData req, Object... aggregates) {
        String name = organizer.groupBy(req);
        RequestStats stats = this.data.get(name);
        if (stats == null) {
            stats = new RequestStats(organizer.useProperties());
        }
        stats.add(req);
    }

    public Map<String, RequestStats> getData() {
        return data;
    }

    public enum RequestGroups {
        
    }
    
    public enum RequestProperties {
        TIME {
            @Override
            public String toString() {
                return "time";
            }
        },

        RESPONSELENGTH {
            @Override
            public String toString() {
                return "responseLength";
            }
        },

        BODYCONTENTLENGTH {
            @Override
            public String toString() {
                return "bodyContentLength";
            }
        },

        CONTENTLENGTH {
            @Override
            public String toString() {
                return "contentLength";
            }
        },

        REMOTELAT {
            @Override
            public String toString() {
                return "remoteLat";
            }
        },

        REMOTELON {
            @Override
            public String toString() {
                return "remoteLon";
            }
        },

        ERRORS {
            @Override
            public String toString() {
                return "errors";
            }
        };
        
    }

    class RequestStats {
        private Map<RequestProperties, Statistics> data = new HashMap<RequestProperties, Statistics>();

        private int count;

        public RequestStats() {
            count = 0;
            data.put(RequestProperties.RESPONSELENGTH, new Statistics());
            data.put(RequestProperties.BODYCONTENTLENGTH, new Statistics());
            data.put(RequestProperties.CONTENTLENGTH, new Statistics());
            data.put(RequestProperties.REMOTELAT, new Statistics());
            data.put(RequestProperties.REMOTELON, new Statistics());
            data.put(RequestProperties.REMOTELON, new Statistics());
            data.put(RequestProperties.ERRORS, new Statistics());
        }

        public RequestStats(final RequestProperties[] properties) {
            count = 0;
            for (RequestProperties prop : properties) {
                data.put(prop, new Statistics());
            }
        }

        public Map<RequestProperties, Statistics> getData() {
            return data;
        }

        public int getCount() {
            return count;
        }

        public void add(RequestData req) {
            ++count;

            Statistics stat = data.get(RequestProperties.REMOTELAT);
            if (stat != null) {
                stat.add(req.getRemoteLat());
                data.put(RequestProperties.REMOTELAT, stat);
            }
            stat = data.get(RequestProperties.REMOTELON);
            if (stat != null) {
                stat.add(req.getRemoteLon());
                data.put(RequestProperties.REMOTELON, stat);
            }
            // Time
            stat = data.get(RequestProperties.TIME);
            if (stat != null) {
                stat.add(req.getTotalTime());
                data.put(RequestProperties.TIME, stat);
            }
            // responseLength
            stat = data.get(RequestProperties.RESPONSELENGTH);
            if (stat != null) {
                stat.add(req.getResponseLength());
                data.put(RequestProperties.RESPONSELENGTH, stat);
            }
            // bodyContentLength
            stat = data.get(RequestProperties.BODYCONTENTLENGTH);
            if (stat != null) {
                stat.add(req.getBodyContentLength());
                data.put(RequestProperties.BODYCONTENTLENGTH, stat);
            }
            // exception
            Throwable t = req.getError();
            stat = data.get(RequestProperties.ERRORS);
            if (stat != null) {
                if (t != null) {
                    // ERROR
                    stat.add(1);
                } else {
                    stat.add(0);
                }
                data.put(RequestProperties.ERRORS, stat);
            }
        }
    }

    interface Organizer {
        String groupBy(RequestData req);

        /**
         * returns the set of properties to add to the statistics map
         * 
         * @param req the request data
         * @return
         */
        RequestProperties[] useProperties();
    }

}