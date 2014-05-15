/**
 * Flow Closeness Class
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
import java.lang.Runnable;

import org.json.JSONObject;

@Path("/flowcloseness")
public class FlowCloseness {
    private final GraphDatabaseService database;
    public FlowCloseness( @Context GraphDatabaseService database ) {
        this.database = database;
    }
    /*
     * Flow Betweeness: RESTful Service...
     * Calculates Flow Betweness in the Current Database.
     * @param target: the id of the target to get the centrality value.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{target}")
    public Response flowCloseness(@PathParam("target") long target) {
        JSONObject obj = new org.json.JSONObject();
        try{
            obj.put("flow-closeness", this.getFlowCloseness(target));
            obj.put("target-node",      target);
        } catch (Exception ex) {
            System.err.println("centralities.FlowCloseness Class: " + ex);
        }
        return Response.ok(obj.toString(), MediaType.APPLICATION_JSON).build();
    }
    /*
     * Calculates the FlowCloseness given a target, using the Maximum Flow Class
     * @param target: The target node to get the centrality...
     */
    public double getFlowCloseness(long targetNodeId){
        double flowSum    = 0.0;
        double closeness  = 0.0;
        Transaction tx = database.beginTx();
        try {
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
                        maxflowObj.getMaxFlow(source.getId(),sink.getId(),targetNodeId);
                        flowSum     =  maxflowObj.getTargetNodeFlow() + flowSum;
                    }
                }
            }
            closeness = flowSum;
        }catch (Exception e) {
            System.err.println("Exception Error: FlowBetweenness Class: " + e);
            tx.failure();
        } finally {
            tx.success();
            tx.close();
        }
        return closeness;
    }
    private double getFlowCloseness(long targetNodeId, long nodeLocalNetwork){
        return 0;
    }
}