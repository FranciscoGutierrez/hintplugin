/**
 * Flow Betweenness Class
 *
 * @author  Francisco Gutiérrez. (fsalvador23@gmail.com)
 * @version 0.1
 * @since 2014-05-01
 */
package org.neo4j.hintplugin.centralities;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.neo4j.graphdb.*;

import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.hintplugin.utils.MaximumFlow;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.lang.Thread;

import org.json.JSONObject;

@Path("/flowbetweenness")
public class FlowBetweenness {
    private final GraphDatabaseService database;
    public FlowBetweenness( @Context GraphDatabaseService database ) {
        this.database = database;
    }
    /*
     * Flow Betweeness: RESTful Service...
     * @param target: the id of the target to get the centrality value.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{target}")
    public Response flowBetweenness(@PathParam("target") long target) {
        JSONObject obj = new org.json.JSONObject();
        try{
            obj.put("flow-betweenness", this.getFlowBetweenness(target));
            obj.put("target-node",      target);
        } catch (Exception ex) {
            System.err.println("MaximumFlowService: " + ex);
        }
        return Response.ok(obj.toString(), MediaType.APPLICATION_JSON).build();
    }
    /*
     * Calculates the FlowBetweeness given a source and sink nodes.
     * @param target: The target node to get the centrality...
     *
     */
    public double getFlowBetweenness(long targetNodeId){
        double maxFlowSum   = 0.0;
        double flowSum      = 0.0;
        double betweenness  = 0.0;
        Transaction tx = database.beginTx();
        try{
            Node                    targetNode  = database.getNodeById(targetNodeId);
            MaximumFlow             maxflowObj  = new MaximumFlow(database);
            Iterable    <Node>      allNodes    = GlobalGraphOperations.at(database).getAllNodes();
            List        <Double>    maxflows    = new ArrayList<Double>();
            //Getting all the nodes...
            for(Node source: allNodes){
                for(Node sink: allNodes){
                    if((source.getId()!= sink.getId())
                       && (source.getId() != targetNodeId)
                       && (sink.getId() != targetNodeId)){
                        //Running Thread smoothly...
                        maxFlowSum  =  maxflowObj.getMaxFlow(source.getId(),sink.getId(),targetNodeId) + maxFlowSum;
                        flowSum     =  maxflowObj.getTargetNodeFlow() + flowSum;
                        /*
                         Thread t = new Thread() {
                         public void run() {
                         maxflows.add(maxflow.getMaxFlow(source.getId(),sink.getId(),0)); //Running Thread smoothly...
                         }
                         };
                         t.start();*/
                    }
                }
            }
            betweenness = flowSum/maxFlowSum;
        }catch (Exception e) {
            System.err.println("Exception Error: FlowBetweenness Class: " + e);
            tx.failure();
        } finally {
            tx.success();
            tx.close();
        }
        return betweenness;
    }
    private double getFlowBetweenness(long targetNodeId, long nodeLocalNetwork){
        return 0;
    }
}