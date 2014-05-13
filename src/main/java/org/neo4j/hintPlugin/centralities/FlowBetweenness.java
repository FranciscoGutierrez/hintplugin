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

import java.util.ArrayList;
import java.util.List;
import java.lang.Thread;

@Path("/flowbetweenness")
public class FlowBetweenness {
    private final GraphDatabaseService database;
    public FlowBetweenness( @Context GraphDatabaseService database ) {
        this.database = database;
    }
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{target}")
    public Response flowBetweenness(@PathParam("target") long target) {
        String json = "{\"flowbetweenness\":" + this.getFlowBetweenness(target) + "}";
        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }
    /*
     * Calculates the FlowBetweeness given a source and sink nodes.
     * @param target    the target node...
     */
    public double getFlowBetweenness(long targetNodeId){
        Node        tnode    = database.getNodeById(targetNodeId);
        final MaximumFlow maxflow  = new MaximumFlow(database);
        List <Node>     flowNode = new ArrayList<Node>();
        final List <Double>   maxflows = new ArrayList<Double>();
        List <Double>   tflow    = new ArrayList<Double>();
        
        Iterable<Node> allGraphNodes = GlobalGraphOperations.at(database).getAllNodes();
        for(Node n : allGraphNodes){
            if(database.getNodeById(targetNodeId).getId() != n.getId()) {
                flowNode.add(n);
            }
        }
        for(final Node source: flowNode){
            for(final Node sink: flowNode){
                if((source.getId()!= sink.getId())
                    && (source.getId() != targetNodeId)
                    && (sink.getId() != targetNodeId)){
                    Thread t = new Thread() {
                        public void run() {
                            maxflows.add(maxflow.getFlow(source.getId(),sink.getId())); //Running Thread smoothly...
                        }
                    };
                    t.start();
                }
            }
        }
        return 0;
    }
    private double getFlowBetweenness(long targetNodeId, long nodeLocalNetwork){
        return 0;
    }
}