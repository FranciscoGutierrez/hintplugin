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
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
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
 * @proyect Reaumobile (Universidad de las Américas Puebla Team)  http://ict.udlap.mx/
 * @version 0.1
 * @since 2014-05-01
 */
@Path("/flowbetweenness")
public class FlowBetweenness {
    private final GraphDatabaseService database;
    
    /**
     * The enum listing all the relationship types allowed in this class.
     */
    private enum Rels implements RelationshipType {
        MAX_FLOW
    }
    /**
     * The Public Constructor to get the Database Service
     */
    public FlowBetweenness(@Context GraphDatabaseService database) {
        this.database = database;
    }
    /**
     * Flow Betweeness: GET RESTful Service...
     * @param target: The long id of the node to get the centrality value.
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
    /**
     * Calculates the FlowBetweeness given a targetNode.
     * @param target: The long id of the target node.
     */
    public double getFlowBetweenness(long targetNodeId){
        double maxFlowSum   = 0.0;
        double flowSum      = 0.0;
        double betweenness  = 0.0;
        double nodeCount    = 0.0;
        final Label poiLabel  = DynamicLabel.label("Poi");
        final Label termLabel = DynamicLabel.label("Term");
        Transaction tx = database.beginTx();
        try {
            Node targetNode = database.getNodeById(targetNodeId);
            for(Node n : GlobalGraphOperations.at(database).getAllNodes()){
                if (n.hasLabel(poiLabel) || n.hasLabel(termLabel)){
                    nodeCount++;
                }
            }
            for(Relationship rel : this.database.traversalDescription()
                .breadthFirst()
                .relationships(Rels.MAX_FLOW)
                .evaluator(Evaluators.excludeStartPosition())
                .traverse(targetNode)
                .relationships()) {
                if (rel.hasProperty("maxflow"))
                    maxFlowSum = (Double)rel.getProperty("maxflow") + maxFlowSum;
            }
            if (targetNode.hasProperty("flow"))
                flowSum = Double.parseDouble(targetNode.getProperty("flow").toString());
            // Freeman's normalized Flow Betweenness...
            betweenness = ((2 * flowSum/maxFlowSum)/(Math.pow(nodeCount,2)-(3*165)+2))*1000;
            betweenness = Math.round(Math.abs(betweenness) * 100.0)/100.0;
            targetNode.setProperty("flowbetweenness",betweenness);
            tx.success();
        }catch (Exception e) {
            System.err.println("**flowBetweenness: " + e);
            tx.failure();
        } finally {
            tx.close();
        }
        return betweenness;
    }
}