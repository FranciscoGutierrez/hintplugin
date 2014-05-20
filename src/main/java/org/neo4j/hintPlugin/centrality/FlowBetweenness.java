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

package org.neo4j.hintplugin.centrality;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.hintplugin.utils.MaximumFlow;
import java.util.ArrayList;
import java.util.List;
import java.lang.Runnable;
import org.json.JSONObject;

/**
 * Flow Betweenness Class
 * @author  Francisco Gutiérrez. (fsalvador23@gmail.com)
 * @version 0.1
 * @since 2014-05-01
 */
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
            obj.put("flowBetweenness", this.getFlowBetweenness(target));
            obj.put("targetNode",      target);
        } catch (Exception ex) {
            System.err.println("centralities.FlowBetweenness Class: " + ex);
        }
        return Response.ok(obj.toString(), MediaType.APPLICATION_JSON).build();
    }
    /*
     * Calculates the FlowBetweeness given a source and sink nodes.
     * @param target: The target node to get the centrality...
     */
    public double getFlowBetweenness(long targetNodeId){
        double maxFlowSum   = 0.0;
        double flowSum      = 0.0;
        double betweenness  = 0.0;
        Transaction tx = database.beginTx();
        try {
            Node targetNode = database.getNodeById(targetNodeId);
            MaximumFlow maxflowObj = new MaximumFlow(database);
            Iterable <Node> allNodes = GlobalGraphOperations.at(database).getAllNodes();
            List <Double> maxflows = new ArrayList<Double>();
            //Getting all the nodes...
            for(Node source: allNodes){
                for(Node sink: allNodes){
                    if((source.getId()!= sink.getId())
                       && (source.getId() != targetNodeId)
                       && (sink.getId() != targetNodeId)){
                        //Running Thread smoothly... ???
                        maxFlowSum = maxflowObj.getMaxFlow(source.getId(),sink.getId(),targetNodeId) + maxFlowSum;
                        flowSum    = maxflowObj.getTargetNodeFlow() + flowSum;
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