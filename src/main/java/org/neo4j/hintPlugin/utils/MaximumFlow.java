/**
 * Maximum Flow Class
 * Returns the Maximum flow value within a graph given a source and sink nodes.
 * @author  Francisco Gutiérrez. (fsalvador23@gmail.com)
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
import static org.neo4j.graphalgo.GraphAlgoFactory.allSimplePaths;
import static org.neo4j.kernel.Traversal.expanderForAllTypes;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;
import java.util.UUID;


import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

@Path("/maximumflow")
public class MaximumFlow {
    
    private final GraphDatabaseService database;
    private Node nSource;
    private Node nSink;
    public int maxDepth = 10000;
    private double targetNodeFlow = 0.0;
    private Map<Long, Double> nodeFlows;
    /*
     * The Public Constructor of this class.
     * @param database: The GraphDatabaseService object needed to feed this class...
     */
    public MaximumFlow(@Context GraphDatabaseService database) {
        this.database = database;
    }
    /*
     * Maximum Flow: RESTful Service...
     * @param source: the id of the source node.
     * @param   sink: the id of the sink node.
     * @param target: the id of the target node to get the flow running through it.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path( "/{source}/{sink}/{target}" )
    public Response maximumflow(@PathParam("source") long source,
                                @PathParam("sink")   long sink,
                                @PathParam("target") long target) {
        JSONObject obj = new org.json.JSONObject();
        try{
            obj.put("source-id",    source);
            obj.put("sink-id",      sink);
            obj.put("maxflow",      this.getMaxFlow(source,sink,target));
            obj.put("target-flow",  this.getTargetNodeFlow());
        } catch (Exception ex) {
            System.err.println("MaximumFlowService: " + ex);
        }
        return Response.ok(obj.toString(), MediaType.APPLICATION_JSON).build();
    }
    /*
     * Calculates the MaximumFlow given a source and sink nodes.
     *
     * @param source    the source node id
     * @param sink      the sink node id
     * @param target    the target node to get the flow that is going through...
     * @return double   as the maximum flow value.
     */
    public double getMaxFlow(long source, long sink, long target){
        double flow = 0.0d;
        double accumulator = 0.0d;
        double min = Double.MAX_VALUE;
        nodeFlows = new HashMap<Long, Double>();
        String uuid = UUID.randomUUID().toString(); //For sake of process uniqueness
        //Declare a simple list to store the flow and get the min.
        List<Double> flows = new ArrayList<Double>();
        Transaction tx = database.beginTx();
        try {
            this.nSource = database.getNodeById(source);
            this.nSink = database.getNodeById(sink);
            //Assing an unique temp property.
            for(org.neo4j.graphdb.Path p : allSimplePaths(expanderForAllTypes(Direction.BOTH),maxDepth)
                .findAllPaths(this.nSource,this.nSink)) {
                for (Relationship r : p.relationships()) {
                    r.setProperty("flw-"+uuid,r.getProperty("weight"));
                }
            }
            //Search for the maximum flow between the source and sink...
            for(org.neo4j.graphdb.Path p : allSimplePaths(expanderForAllTypes(Direction.BOTH),maxDepth).findAllPaths(this.nSource,this.nSink)) {
                //Look for all the properties and put them into a List
                for (Relationship r : p.relationships()) {
                    flows.add((Double)r.getProperty("flw-"+uuid));
                }
                /*
                 * Calculate the min, because:
                 * flow = Collections.min(flows); 
                 * Doesn't work with double primitives...
                 */
                for (double ds : flows) {
                    if (ds < min ) flow=ds;
                }
                // Look for the flow that passes for desired Node
                flows.clear();
                accumulator = accumulator + flow;
                // Setup the flow values for the next round...
                for (Relationship r: p.relationships()){
                    r.setProperty("flw-"+uuid,(Double)r.getProperty("flw-"+ uuid.toString()) - flow);
                }
                // Save the flow...
                for(Node n : p.nodes()){
                    if( (n!= p.startNode()) && (n!=p.endNode()) )
                        if(n.getId() == target)
                            targetNodeFlow = flow;
                }
            }
            //Remove the temp property...
            for(org.neo4j.graphdb.Path p : allSimplePaths(expanderForAllTypes(Direction.BOTH),maxDepth).findAllPaths(this.nSource,this.nSink)) {
                for (Relationship r : p.relationships()) {
                    r.removeProperty("flw-"+uuid);
                }
            }
            tx.success();
        } catch (Exception e) {
            System.err.println("org.neo4j.hintplugin.utils.MaximumFlow.getFlow: " + e);
            tx.failure();
        } finally {
             tx.close();
        }
        return accumulator;
    }
    
    public double getTargetNodeFlow(){
        return this.targetNodeFlow;
    }
}