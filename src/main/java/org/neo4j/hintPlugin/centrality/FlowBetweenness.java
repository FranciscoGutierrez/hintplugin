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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.hintplugin.utils.MaximumFlow;
import java.util.ArrayList;
import java.util.List;
import java.lang.Runnable;
import org.json.JSONObject;
import java.lang.Math;

/**
 * Flow Betweenness Class
 * @author  Francisco Gutiérrez. (fsalvador23@gmail.com)
 * @version 0.1
 * @since 2014-05-01
 */
@Path("/flowbetweenness")
public class FlowBetweenness {
    private final GraphDatabaseService database;
    /*
     * The enum listing all the relationship types allowed in database.
     */
    private enum Rels implements RelationshipType {
        KNOWS, HAS_TERM, MAX_FLOW
    }
    /*
     * The Public Constructor...
     */
    public FlowBetweenness(@Context GraphDatabaseService database) {
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
     * Calculates the FlowBetweeness given a targetNode.
     * @param target: The target node to get the centrality...
     */
    public double getFlowBetweenness(long targetNodeId){
        double maxFlowSum   = 0.0;
        double flowSum      = 0.0;
        double betweenness  = 0.0;
        Transaction tx = database.beginTx();
        try {
            Node targetNode = database.getNodeById(targetNodeId);
            for(Relationship rel : this.database.traversalDescription()
                .depthFirst()
                .evaluator(Evaluators.endNodeIs(Evaluation.EXCLUDE_AND_CONTINUE,
                                                Evaluation.INCLUDE_AND_CONTINUE,
                                                targetNode))
                .relationships(Rels.MAX_FLOW)
                .evaluator(Evaluators.excludeStartPosition())
                .uniqueness(Uniqueness.NODE_PATH)
                .traverse(targetNode)
                .relationships()) {
                if (rel.hasProperty("maxflow"))
                    maxFlowSum = rel.getProperty("maxflow") + maxFlowSum;
            }
            if (targetNode.hasProperty("maxflow"))
                flowSum = targetNode.getProperty("flow");
            betweenness = flowSum/maxFlowSum;
        }catch (Exception e) {
            System.err.println("Exception Error: FlowBetweenness Class: " + e);
            tx.failure();
        } finally {
            tx.success();
            tx.close();
        }
        return Math.round(Math.abs(betweenness)*100.0)/100.0;;
    }
}