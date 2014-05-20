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
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.json.JSONObject;
import static junit.framework.Assert.assertEquals;

public class EccentricityTest {
    private GraphDatabaseAPI db;
    private CommunityNeoServer server;
    
    enum MyRelationshipTypes implements RelationshipType{
        CONTAINED_IN, KNOWS
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
    public void shouldReturnAllTheNodes() {
        Transaction tx = db.beginTx();
        Node a = db.createNode();
        Node b = db.createNode();
        Node c = db.createNode();
        Node d = db.createNode();
        Node e = db.createNode();
        a.createRelationshipTo(c, MyRelationshipTypes.KNOWS).setProperty("weight",1);
        a.createRelationshipTo(d, MyRelationshipTypes.KNOWS).setProperty("weight",1);
        b.createRelationshipTo(c, MyRelationshipTypes.KNOWS).setProperty("weight",1);
        b.createRelationshipTo(d, MyRelationshipTypes.KNOWS).setProperty("weight",1);
        a.createRelationshipTo(e, MyRelationshipTypes.KNOWS).setProperty("weight",1);
        
        tx.success();
        tx.close();
        int statusCode = 0;
        String node_a = "4";
        String address = server.baseUri().toString()+
        "hintplugin/centrality/eccentricity/"+ node_a;
        try{
            URL url = new URL(address);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestMethod("GET");
            http.connect();
            statusCode = http.getResponseCode();
            
            StringBuffer text = new StringBuffer();
            InputStreamReader in = new InputStreamReader((InputStream) http.getContent());
            BufferedReader buff = new BufferedReader(in);
            String line = "";
            while (line != null) {
                line = buff.readLine();
                text.append(line + " ");
            }
            JSONObject obj = new JSONObject(text.toString());
            System.out.println("*********EccentricityJSON: " +
                               obj.optDouble("eccentricity"));
            
        }
        catch(IOException ex){
        }
        assertEquals("200",statusCode + "");
    }
    private Client jerseyClient() {
        DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
        defaultClientConfig.getClasses().add(JacksonJsonProvider.class);
        return Client.create(defaultClientConfig);
    }
}