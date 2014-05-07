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
        Node a = db.createNode();
        Node b = db.createNode();
        Node c = db.createNode();
        Node d = db.createNode();
        Node e = db.createNode();
        Node f = db.createNode();
        
        a.createRelationshipTo(c, MyRelationshipTypes.KNOWS).setProperty("weight",1);
        a.createRelationshipTo(b, MyRelationshipTypes.KNOWS).setProperty("weight",3);
        a.createRelationshipTo(d, MyRelationshipTypes.KNOWS).setProperty("weight",2);
        b.createRelationshipTo(c, MyRelationshipTypes.KNOWS).setProperty("weight",3);
        c.createRelationshipTo(d, MyRelationshipTypes.KNOWS).setProperty("weight",2);
        c.createRelationshipTo(e, MyRelationshipTypes.KNOWS).setProperty("weight",2);
/*
        Node s = db.createNode();
        Node o = db.createNode();
        Node p = db.createNode();
        Node q = db.createNode();
        Node r = db.createNode();
        Node t = db.createNode();
        
        s.createRelationshipTo(o, MyRelationshipTypes.KNOWS).setProperty("weight",3);
        s.createRelationshipTo(p, MyRelationshipTypes.KNOWS).setProperty("weight",3);
        o.createRelationshipTo(p, MyRelationshipTypes.KNOWS).setProperty("weight",2);
        o.createRelationshipTo(q, MyRelationshipTypes.KNOWS).setProperty("weight",3);
        p.createRelationshipTo(r, MyRelationshipTypes.KNOWS).setProperty("weight",2);
        r.createRelationshipTo(t, MyRelationshipTypes.KNOWS).setProperty("weight",3);
        q.createRelationshipTo(r, MyRelationshipTypes.KNOWS).setProperty("weight",4);
        q.createRelationshipTo(t, MyRelationshipTypes.KNOWS).setProperty("weight",2);
 */
        tx.success();
        tx.close();

        int statusCode = 0;
        String node_a = "0";
        String node_b = "4";
        String address = server.baseUri().toString()+
                        "hintplugin/utils/maximumflow/"+
                        node_a + "/" + node_b;
        
        try{
            URL url = new URL(address);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            statusCode = http.getResponseCode();
        } catch(Exception ex){
                System.out.println("***** ERROR: " + ex);
        }
        assertEquals("200",statusCode + "");
    }
    
    private Client jerseyClient() {
        DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
        defaultClientConfig.getClasses().add(JacksonJsonProvider.class);
        return Client.create(defaultClientConfig);
    }
}