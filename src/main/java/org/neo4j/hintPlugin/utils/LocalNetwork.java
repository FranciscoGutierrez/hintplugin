/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Francisco G.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.neo4j.hintplugin.utils;

import java.nio.charset.Charset;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.neo4j.cypher.export.CypherResultSubGraph;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.export.SubGraph;
import org.neo4j.cypher.CypherException;
/**
 * Local Network Class
 *
 * Local Network Class is used to return a big JSON response.
 * The response includes all the centralities and metrics expected of a user
 * local network. This class does not save anything into the database, because
 * setting all the values for every node in each local network will turn out
 * into a messy network.
 *
 * This class extracts a subgraph using a Cypher query, then returns all
 * data metrics in a JSON File
 *
 * @author  Francisco Gutiérrez. (fsalvador23@gmail.com)
 * @version 0.1
 * @since 2014-05-01
 */

@Path("/localnetwork")
public class LocalNetwork {
    
    private final GraphDatabaseService database;
    
    private enum Rels implements RelationshipType {
        LIKES_TERM, KNOWS, HAS_TERM, MAX_FLOW
    }
    
    /**
     * The Public Constructor of this class.
     * @param database: The GraphDatabaseService object.
     */
    
    public LocalNetwork(@Context GraphDatabaseService database) {
        this.database = database;
    }
    
    /**
     * Maximum Flow: RESTful Service...
     * @param targetNodeID: the target node to subtract the local network.
     */
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path( "/{nodeID}" )
    public Response maximumflow(@PathParam("nodeID") long nodeID){
        Gson       gson = new GsonBuilder().create();
        JsonObject obj  = new JsonObject();
        try{
            obj.addProperty("localnetwork", 12);
            obj.addProperty("targetNode", nodeID);
        } catch (Exception ex) {
            System.err.println("utils.LocalNetwork Class: " + ex);
        }
        return Response.ok(gson.toJson(obj), MediaType.APPLICATION_JSON)
        .header("X-Stream", "true")
        .build();
    }
    /**
     * Returns a local network given a NodeID
     */
    /*
    private SubGraph getLocalNetwork(long nodeId){
      ExecutionEngine engine = new ExecutionEngine(this.database);
        ExecutionResult result;
        String query = "match (n:Person)--(t:Term)-[r:HAS_TERM]-(p:Poi)" +
                       "WHERE id(n)=" + nodeId + " return x,t,p";
        try (Transaction ignored = db.beginTx())  {
            result = engine.execute(query);
        } catch (Exception e){
            
        }
        return CypherResultSubGraph.from(this.execute(query),this.database,false);
    }
    */
}