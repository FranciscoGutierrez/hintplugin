/**
 * Maximum Flow Class
 * Returns the Maximum flow value within a graph given a source and sink nodes.
 * @author  Francisco Gutiérrez.
 * @version 0.1
 * @since 2014-05-01
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
import static org.neo4j.graphalgo.GraphAlgoFactory.allPaths;
import static org.neo4j.kernel.Traversal.expanderForAllTypes;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

@Path("/maximumflow")
public class MaximumFlow {

    private final GraphDatabaseService database;
    private Node nSource;
    private Node nSink;

    enum MyRelationshipTypes implements RelationshipType
    {
        KNOWS, IS_SIMILAR
    }

    public MaximumFlow( @Context GraphDatabaseService database ) {
        this.database = database;
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path( "/{source}/{sink}" )
    public Response maximumflow(@PathParam("source") long source, @PathParam("sink") long sink) {
        JSONObject obj = new org.json.JSONObject();
        try {
            obj.put("maxflow", this.getFlow(source,sink));
            obj.put("source-id", source);
            obj.put("sink-id", sink);
        } catch (Exception ex) {}
        
        return Response.ok(obj.toString(), MediaType.APPLICATION_JSON).build();
    }
    
    /*
     * Calculates the MaximumFlow given a source and sink nodes.
     *
     * @param source    the source node id
     * @param sink      the sink node id
     * @return double   as the maximum flow value.
     */
    
    private double getFlow(long source, long sink){
        int maxDepth = 10000;
        int flow = 0;
        int accumulator = 0;
        //Declare a simple list to store the flow and get the min.
        List<Integer> flows = new ArrayList<Integer>();
        Transaction tx = database.beginTx();
        try {
            this.nSource = database.getNodeById(source);
            this.nSink = database.getNodeById(sink);
            //First of all set a temp property.
            for(org.neo4j.graphdb.Path p : allPaths(expanderForAllTypes(Direction.BOTH),maxDepth).findAllPaths(this.nSource,this.nSink)) {
                for (Relationship r : p.relationships()) {
                    r.setProperty("tmp-weight",r.getProperty("weight"));
                }
            }
            //Search for the maximum flow between the source and sink...
            for(org.neo4j.graphdb.Path p : allPaths(expanderForAllTypes(Direction.BOTH),maxDepth).findAllPaths(this.nSource,this.nSink)) {
                for (Relationship r : p.relationships()) {
                    flows.add((Integer)r.getProperty("tmp-weight"));
                }
                flow = Collections.min(flows);
                flows.clear();
                accumulator = accumulator + flow;
                for (Relationship r: p.relationships()){
                    r.setProperty("tmp-weight",(Integer)r.getProperty("tmp-weight") - flow);
                }
            }
            //Remove the temp property
            for(org.neo4j.graphdb.Path p : allPaths(expanderForAllTypes(Direction.BOTH),maxDepth).findAllPaths(this.nSource,this.nSink)) {
                for (Relationship r : p.relationships()) {
                    r.removeProperty("tmp-weight");
                }
            }
            //At this point Accumulator should have the max-flow value...
            tx.success();
        } catch (Exception e) {
            System.err.println("org.neo4j.hintplugin.utils.MaximumFlow.getFlow: " + e);
            tx.failure();
        } finally {
             tx.close();
        }
        return accumulator;
    }
}