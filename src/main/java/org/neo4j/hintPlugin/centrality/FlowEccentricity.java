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
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.hintplugin.utils.MaximumFlow;
import static org.neo4j.graphalgo.GraphAlgoFactory.shortestPath;
import static org.neo4j.kernel.Traversal.expanderForAllTypes;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;
import java.lang.Runnable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
/**
 *
 * Eccentricity is defined as the maximum distance 
 * to any other node in the graph.
 *
 * @author  Francisco Gutiérrez. (fsalvador23@gmail.com)
 * @version 0.1
 * @since 2014-05-01
 */
@Path("/floweccentricity")
public class FlowEccentricity {
    private final GraphDatabaseService database;
    public int maxDepth = 10000;
    private double maxValue = 0;
    public FlowEccentricity( @Context GraphDatabaseService database ) {
        this.database = database;
    }
    /*
     * Eccentricity: RESTful Service...
     * @param target: the id of the target to get the centrality value.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{targetNodeId}")
    public Response eccentricity(@PathParam("targetNodeId") long targetNodeId) {
        Gson       gson = new GsonBuilder().create();
        JsonObject obj  = new JsonObject();
        double flowEccentricity = this.getFlowEccentricity(targetNodeId);
        try{
            obj.addProperty("flowEccentricity", flowEccentricity);
            obj.addProperty("targetNode", targetNodeId);
            obj.addProperty("pathMaxLength", maxValue);
        } catch (Exception ex) {
            System.err.println("centrality.Eccentricity Class: " + ex);
        }
        return Response.ok(gson.toJson(obj),MediaType.APPLICATION_JSON).build();
    }
    /*
     * Calculates the Eccentricity given a target.
     * @param targetNodeId: The target node to get the centrality.
     */
    public double getFlowEccentricity(long targetNodeId){
        this.maxValue = 0;
        Transaction tx = database.beginTx();
        try {
            Node targetNode  = database.getNodeById(targetNodeId);
            /* Get all other nodes in Graph (ignoring targetNodeId) */
            for (Relationship rel : targetNode.getRelationships()){
                if(rel.hasProperty("maxflow")){
                    if((Double)rel.getProperty("maxflow") > this.maxValue){
                        this.maxValue = (Double)rel.getProperty("maxflow");
                    }
                }
            }
            targetNode.setProperty("floweccentricity",this.maxValue);
        }catch (Exception e) {
            System.err.println("Exception Error: FlowBetweenness Class: " + e);
            tx.failure();
        } finally {
            tx.success();
            tx.close();
        }
        return Math.round((1.0/Math.abs(maxValue))*100.0)/100.0;
    }
}