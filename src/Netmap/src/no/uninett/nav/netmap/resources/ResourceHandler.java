package no.uninett.nav.netmap.resources;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import prefuse.data.io.DataIOException;

public class ResourceHandler extends Thread {


    private HashMap allocated_resources;

    public ResourceHandler() {
        allocated_resources = new HashMap();

        // We trust all certificates
        /*TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {

                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setFollowRedirects(true);
        } catch (Exception e) {
            System.out.println("Failed to create SSL-socket");
            e.printStackTrace();
        }*/
    }

    public ArrayList<String> getAvailableCategories() throws Exception {
        ArrayList<String> ret = new ArrayList<String>();

        URL url = null;
        HttpURLConnection conn;

        url = new URL(no.uninett.nav.netmap.Main.getBaseURL().toString() + "/catids");
        conn = (HttpURLConnection) url.openConnection();

        conn.setRequestProperty("Cookie", no.uninett.nav.netmap.Main.getSessionID());
        conn.setRequestProperty("Accept-Charset", "utf-8");

        DataInputStream dis = new DataInputStream(conn.getInputStream());
        String cats = dis.readLine();
        for (String cat : cats.split(",")) {
            if (cat != null && !cat.equals("")) {
                ret.add(cat);
            }
        }
        return ret;
    }
    
    public ArrayList<String> getAvailableLinkTypes() throws Exception {
        ArrayList<String> ret = new ArrayList<String>();

        URL url = null;
        HttpURLConnection conn;

        url = new URL(no.uninett.nav.netmap.Main.getBaseURL().toString() + "/linktypes");
        conn = (HttpURLConnection) url.openConnection();

        conn.setRequestProperty("Cookie", no.uninett.nav.netmap.Main.getSessionID());
        conn.setRequestProperty("Accept-Charset", "utf-8");

        DataInputStream dis = new DataInputStream(conn.getInputStream());
        String types = dis.readLine();
        for (String type : types.split(",")) {
            if (type != null && !type.equals("")) {
                ret.add(type);
            }
        }
        return ret;
    }

    public prefuse.data.Graph getGraphFromURL(URL url) throws DataIOException {

            System.out.println("Fetching GraphML from " + url.toString());
            // First see if we have the graph in our cache
            if (allocated_resources.containsKey(url.toString())) {
                    return (prefuse.data.Graph) allocated_resources.get(url);
            }

            HttpURLConnection conn;
            try {
                    conn = (HttpURLConnection) url.openConnection();
            } catch (IOException ex) {
                    throw new DataIOException(ex.fillInStackTrace());
            }
            conn.setRequestProperty("Cookie", no.uninett.nav.netmap.Main.getSessionID());
            conn.setRequestProperty("Accept-Charset", "utf-8");
            System.out.println("with cookie: " + conn.getRequestProperty("Cookie"));
            prefuse.data.Graph ret = null;

            try {
                    InputStreamReader dis = new InputStreamReader(conn.getInputStream(), "UTF-8");
                    String s_graph = "";
                    StringBuffer buffer = new StringBuffer();
                    Reader in = new BufferedReader(dis);
                    int ch;
                    while ((ch = in.read()) > -1) {
                            buffer.append((char)ch);
                    }
                    in.close();
                    s_graph = buffer.toString();


                    prefuse.data.io.GraphMLReader reader = new prefuse.data.io.GraphMLReader();

                    try {
                            ret = reader.readGraph(
                                            new ByteArrayInputStream(s_graph.getBytes())
                                            );
                    } catch (Exception e) {
                            e.printStackTrace();
                            throw new DataIOException(e.fillInStackTrace());
                    }

            } catch (Exception e) {

                    throw new DataIOException(e.fillInStackTrace());
            }

            allocated_resources.put(url.toString(), ret);

            return ret;
    }
}
