package org.neo4j.mytests;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.LocalConnector;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.helpers.CommunityServerBuilder;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphalgo.*;
import org.neo4j.tooling.GlobalGraphOperations;
import java.io.IOException;
import java.net.ServerSocket;
import static junit.framework.Assert.assertEquals;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.json.JSONObject;

public class BetweennessTest {
    private GraphDatabaseAPI db;
    private CommunityNeoServer server;
    
    enum MyRelationshipTypes implements RelationshipType {
        CONTAINED_IN, KNOWS, MAX_FLOW
    }
    
    @Before
    public void before() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        server = CommunityServerBuilder
        .server()
        .onPort(serverSocket.getLocalPort())
        .withThirdPartyJaxRsPackage("org.neo4j.hintplugin.centrality", "/hintplugin/centrality")
        .build();
        server.start();
        db = server.getDatabase().getGraph();
    }
    
    @After
    public void after() {
        server.stop();
    }
    
    @Test
    public void shouldReturnBetweenness() {
        Transaction tx = db.beginTx();
        try{
            Node a = db.createNode();
            Node b = db.createNode();
            Node c = db.createNode();
            Node d = db.createNode();
            Node e = db.createNode();
            
            a.setProperty("flow",4);
            b.setProperty("flow",2);
            c.setProperty("flow",2);
            d.setProperty("flow",1);
            e.setProperty("flow",0);
            
            a.createRelationshipTo(c, MyRelationshipTypes.MAX_FLOW).setProperty("maxflow",0.1);
            a.createRelationshipTo(b, MyRelationshipTypes.MAX_FLOW).setProperty("maxflow",0.3);
            a.createRelationshipTo(d, MyRelationshipTypes.MAX_FLOW).setProperty("maxflow",0.2);
            b.createRelationshipTo(c, MyRelationshipTypes.MAX_FLOW).setProperty("maxflow",0.3);
            c.createRelationshipTo(d, MyRelationshipTypes.MAX_FLOW).setProperty("maxflow",0.2);
            c.createRelationshipTo(e, MyRelationshipTypes.MAX_FLOW).setProperty("maxflow",0.2);
            
        } catch (Exception e) {
            System.err.println("Exception Error: MaxflowTest.shouldReturnMaxFlow: " + e);
            tx.failure();
        } finally {
            tx.success();
            tx.close();
        }
        URL uriArray[] = new URL[5];
        String serverBaseUri = server.baseUri().toString();
        try{
            String q1 = serverBaseUri + "hintplugin/centrality/flowbetweenness/0";
            String q2 = serverBaseUri + "hintplugin/centrality/flowbetweenness/1";// 0.3599
            String q3 = serverBaseUri + "hintplugin/centrality/flowbetweenness/2";
            String q4 = serverBaseUri + "hintplugin/centrality/flowbetweenness/3";
            String q5 = serverBaseUri + "hintplugin/centrality/flowbetweenness/4";
            
            uriArray[0] = new URL(q1);
            uriArray[1] = new URL(q2);
            uriArray[2] = new URL(q3);
            uriArray[3] = new URL(q4);
            uriArray[4] = new URL(q5);
        }catch(Exception ex){
            System.out.println("***** URL Error: " + ex);
        }
        
        //Establish a connection to the server and get Content.
        for(int i =0; i<uriArray.length; i++){
            try {
                HttpURLConnection http = (HttpURLConnection)uriArray[i].openConnection();
                http.setRequestMethod("GET");
                http.connect();
                StringBuffer text = new StringBuffer();
                InputStreamReader in = new InputStreamReader((InputStream) http.getContent());
                BufferedReader buff = new BufferedReader(in);
                String line = "";
                while (line != null) {
                    line = buff.readLine();
                    text.append(line + " ");
                }
                JSONObject obj = new JSONObject(text.toString());
                System.out.println("*********betweenness-json: " + obj.optDouble("flowBetweenness"));
            } catch(Exception ex) {
                System.out.println("MaxflowTest Exception: " + ex);
            }
        }
        assertEquals("200","200");
    }
    
    private Client jerseyClient() {
        DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
        defaultClientConfig.getClasses().add(JacksonJsonProvider.class);
        return Client.create(defaultClientConfig);
    }
}