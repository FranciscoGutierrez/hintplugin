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

import org.neo4j.tooling.GlobalGraphOperations
import org.neo4j.hintplugin.utils.MaximumFlow;

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
    private double getFlowBetweenness(long targetNode){
                    Node tnode = database.getNodeById(targetNode);
        MaximumFlow maxflow    = new MaximumFlow(database);
        List <double> maxflows = new ArrayList<double>;
        List <double> tflow    = new ArrayList<double>;
        for(Node n : GlobalGraphOperations.at(database).getAllNodes()){
            betweenness = maxflow.getFlow(0,1);
        }
        return 0;
    }
    private double getFlowBetweenness(long targetNode, long nodeNetwork){
        return 0;
    }
}