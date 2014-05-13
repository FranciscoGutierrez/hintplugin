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

@Path("/maximumflow")
public class MaximumFlow {
    
    private final GraphDatabaseService database;
    private Node nSource;
    private Node nSink;
    public int maxDepth = 10000;
    private Iterable<P> pList;
    private Map<long, double> nodeFlows;
    
    public MaximumFlow(@Context GraphDatabaseService database) {
        this.database = database;
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path( "/{source}/{sink}" )
    public Response maximumflow(@PathParam("source") long source, @PathParam("sink") long sink) {
        
        this.nSource = database.getNodeById(source);
        this.nSink = database.getNodeById(sink);
        
        pList = allSimplePaths(expanderForAllTypes(Direction.BOTH),maxDepth)
        .findAllPaths(this.nSource,this.nSink);
        
        JSONObject obj = new org.json.JSONObject();
        try {
            obj.put("maxflow", this.getFlow(source,sink));
            obj.put("source-id", source);
            obj.put("sink-id", sink);
        } catch (Exception ex) {
            System.err.println("org.neo4j.hintplugin.utils.MaximumFlow " + ex);
        }
        return Response.ok(obj.toString(), MediaType.APPLICATION_JSON).build();
    }
    /*
     * assignTempProperty
     * @param String uuid;
     */
    private void assignTempProperty(UUID uuid){
        for(org.neo4j.graphdb.Path p : pList) {
            for (Relationship r : p.relationships()) {
                r.setProperty("flw-"+this.uuid.toString(),r.getProperty("weight"));
            }
        }
    }
    /*
     * removeTempProperty
     * @param UUID uuid;
     */
    private void removeTempProperty(UUID uuid) {
        for(org.neo4j.graphdb.Path p : pList) {
            for (Relationship r : p.relationships()) {
                r.removeProperty("flw-"+ this.uuid.toString());
            }
        }
    }
    /*
     * Calculates the MaximumFlow given a source and sink nodes.
     *
     * @param source    the source node id
     * @param sink      the sink node id
     * @return double   as the maximum flow value.
     */
    public double getFlow(long source, long sink){
        double flow = 0.0d;
        double accumulator = 0.0d;
        double min = Double.MAX_VALUE;
        nodeFlows = new HashMap<long, double>();
        //For sake of process uniqueness
        //Declare a simple list to store the flow and get the min.
        List<Double> flows = new ArrayList<Double>();
        Transaction tx = database.beginTx();
        try {
            this.assignTempProperty(UUID.randomUUID());
            //Search for the maximum flow between the source and sink...
            for(org.neo4j.graphdb.Path p : allSimplePaths(expanderForAllTypes(Direction.BOTH),maxDepth).findAllPaths(this.nSource,this.nSink)) {
                //Look for all the properties and put them into a List
                for (Relationship r : p.relationships()) {
                    flows.add((Double)r.getProperty("flw-"+this.uuid));
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
                    r.setProperty("flw-"+this.uuid,(Double)r.getProperty("flw-"+ this.uuid) - flow);
                }
                // Save the flow...
                for(Node n : p.nodes()){
                        System.out.println("Flow: " + flow + " : " + n.getId());
                }
            }
            this.removeTempProperty(UUID.randomUUID());
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