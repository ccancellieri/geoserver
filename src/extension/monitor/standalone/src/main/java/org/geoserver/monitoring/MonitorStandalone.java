package org.geoserver.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.hib.HibernateMonitorDAO2;
import org.geoserver.monitor.hib.HibernateMonitorDAO2.Sync;
import org.geotools.util.logging.Logging;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MonitorStandalone {
    private static final Logger LOGGER = Logging.getLogger("org.geoserver.monitor");

    /**
     * @param args
     * @throws InterruptedException
     * @throws NamingException
     */
    public static void main(String[] args) throws InterruptedException, NamingException {
        // http://docs.oracle.com/javase/6/docs/api/javax/naming/InitialContext.html
//        Hashtable props = new Hashtable();
//        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.fscontext.RefFSContextFactory");
//         props.put(Context.PROVIDER_URL, "jnp://localhost:1099");
//        Context c = new InitialContext(props);

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
                "standaloneContext.xml");

        HibernateMonitorDAO2 monitor = ctx.getBean(HibernateMonitorDAO2.class);
        monitor.setSync(Sync.SYNC);
        List<RequestData> datas = new ArrayList<RequestData>();
        RequestData d = new RequestData();
        d.setError(new Exception("PUPPA"));
        d.setTotalTime(10);
        datas.add(d);

        for (RequestData data : datas) {
            monitor.add(data);
        }

        System.exit(0);
    }

    // private static void geoserverWeb() throws InterruptedException{
    // // open/read the application context file
    // final XmlWebApplicationContext wctx = new XmlWebApplicationContext();
    // wctx.setConfigLocation("classpath*:standaloneContext.xml");
    // ServletContext mockServletContext = new MockServletContext("webapp");
    //
    // wctx.setServletContext(mockServletContext);
    //
    // wctx.refresh();
    //
    // while (true) {
    // Thread.sleep(10000);
    // RequestData req = new RequestData();
    //
    // wctx.publishEvent(new ServletRequestHandledEvent(req, req.getHost(),
    // req.getRemoteHost(), req.getHttpMethod(), req.getService(), Long.toString(req
    // .getId()), req.getRemoteUser(), req.getTotalTime()));
    // }
    //
    // }

}
