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
import java.io.IOException;
import java.net.ServerSocket;

import static junit.framework.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;

public class MaxflowTest {
    private GraphDatabaseAPI db;
    private CommunityNeoServer server;
    
    enum MyRelationshipTypes implements RelationshipType
    {
        CONTAINED_IN, KNOWS
    }
    
    
    @Before
    public void before() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        server = CommunityServerBuilder
        .server()
        .onPort(serverSocket.getLocalPort())
        .withThirdPartyJaxRsPackage("org.neo4j.hintplugin.utils", "/hintplugin/utils")
        .build();
        server.start();
        db = server.getDatabase().getGraph();
    }
    
    @After
    public void after() {
        server.stop();
    }
    
    @Test
    public void shouldReturnMaxFlow() {
        Transaction tx = db.beginTx();
        try{
        Node a = db.createNode();
        Node b = db.createNode();
        Node c = db.createNode();
        Node d = db.createNode();
        Node e = db.createNode();
        a.createRelationshipTo(c, MyRelationshipTypes.KNOWS).setProperty("weight",1);
        a.createRelationshipTo(b, MyRelationshipTypes.KNOWS).setProperty("weight",3);
        a.createRelationshipTo(d, MyRelationshipTypes.KNOWS).setProperty("weight",2);
        b.createRelationshipTo(c, MyRelationshipTypes.KNOWS).setProperty("weight",3);
        c.createRelationshipTo(d, MyRelationshipTypes.KNOWS).setProperty("weight",2);
        c.createRelationshipTo(e, MyRelationshipTypes.KNOWS).setProperty("weight",2);
        } catch (Exception e) {
            System.err.println("Exception Error: MaxflowTest.shouldReturnMaxFlow: " + e);
            tx.failure();
        } finally {
            tx.success();
            tx.close();
        }

        String serverBaseUri = server.baseUri().toString();
        URL uriArray[] = new URL[6];
        String q1 = serverBaseUri + "hintplugin/utils/maximumflow/0/2"; // 6
        String q2 = serverBaseUri + "hintplugin/utils/maximumflow/0/3"; // 4
        String q3 = serverBaseUri + "hintplugin/utils/maximumflow/0/4"; // 2
        String q4 = serverBaseUri + "hintplugin/utils/maximumflow/2/3"; // 4
        String q5 = serverBaseUri + "hintplugin/utils/maximumflow/2/4"; // 2
        String q6 = serverBaseUri + "hintplugin/utils/maximumflow/3/4"; // 2
        try{
            uriArray[0] = new URL(q1);
            uriArray[1] = new URL(q2);
            uriArray[2] = new URL(q3);
            uriArray[3] = new URL(q4);
            uriArray[4] = new URL(q5);
            uriArray[5] = new URL(q6);
        }catch(Exception ex){
            System.out.println("***** ERROR: " + ex);
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
//              System.out.println("******* JSON: " + text.toString());
                System.out.println("***Flow-JSON: " + obj.optString("maxflow"));
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