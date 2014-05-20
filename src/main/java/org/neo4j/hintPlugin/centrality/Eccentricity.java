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
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.graphalgo.GraphAlgoFactory;
import static org.neo4j.graphalgo.GraphAlgoFactory.shortestPath;
import static org.neo4j.kernel.Traversal.expanderForAllTypes;
import java.util.ArrayList;
import java.util.List;
import java.lang.Runnable;
import org.json.JSONObject; // Must be changed to Gson

/**
 * Eccentricity Class: This can be used to calculate eccentricity of nodes.
 * Eccentricity is defined as the maximum distance to any other node in the graph.
 * @author  Francisco Gutiérrez. (fsalvador23@gmail.com)
 * @version 0.1
 * @since 2014-05-01
 */
@Path("/eccentricity")
public class Eccentricity {
    private final GraphDatabaseService database;
    public int maxDepth = 10000;
    public Eccentricity( @Context GraphDatabaseService database ) {
        this.database = database;
    }
    /*
     * Flow Betweeness: RESTful Service...
     * Calculates Flow Betweness in the Current Database.
     * @param target: the id of the target to get the centrality value.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{targetNodeId}")
    public Response eccentricity(@PathParam("targetNodeId") long targetNodeId) {
        JSONObject obj = new org.json.JSONObject();
        try{
            obj.put("eccentricity",this.getEccentricity(targetNodeId));
            obj.put("target-node", targetNodeId);
        } catch (Exception ex) {
            System.err.println("centrality.Eccentricity Class: " + ex);
        }
        return Response.ok(obj.toString(), MediaType.APPLICATION_JSON).build();
    }
    /*
     * Calculates the Eccentricity given a target.
     * @param targetNodeId: The target node to get the centrality.
     */
    public double getEccentricity(long targetNodeId){
        int maxValue = 0;
        Transaction tx = database.beginTx();
        try {
            Node targetNode  = database.getNodeById(targetNodeId);
            for (Node n : GlobalGraphOperations.at(database).getAllNodes()){
                if(n.getId() != targetNodeId){
                    for (org.neo4j.graphdb.Path p : shortestPath(expanderForAllTypes(Direction.BOTH),maxDepth).findAllPaths(targetNode,n)){
                        if (p.length() > maxValue) {
                            maxValue = p.length();
                        }
                    }
                }
            }
        }catch (Exception e) {
            System.err.println("Exception Error: FlowBetweenness Class: " + e);
            tx.failure();
        } finally {
            tx.success();
            tx.close();
        }
        return 1/maxValue;
    }
}