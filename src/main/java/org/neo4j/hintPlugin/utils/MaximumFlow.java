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
import static org.neo4j.kernel.Traversal.expanderForAllTypes;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Expander;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Direction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.json.JSONObject;
import java.lang.Math;

/**
 * Maximum Flow Class
 * Returns the Maximum flow value within a graph given a source and sink nodes.
 * @author  Francisco Gutiérrez. (fsalvador23@gmail.com)
 * @version 0.1
 * @since 2014-05-01
 */

@Path("/maximumflow")
public class MaximumFlow {
    private final GraphDatabaseService database;
    private double targetNodeFlow;
    private Node nSource;
    private Node nSink;
    private UUID uuid;
    
    private enum Rels implements RelationshipType {
        CONTAINED_IN, KNOWS, HAS_TERM, MAX_FLOW
    }
    
    /*
     * The Public Constructor of this class.
     * @param database: The GraphDatabaseService object needed to feed this class...
     */
    public MaximumFlow(@Context GraphDatabaseService database) {
        this.database = database;
    }
    /*
     * Maximum Flow: RESTful Service...
     * @param source: the id of the source node.
     * @param   sink: the id of the sink node.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path( "/{source}/{sink}" )
    public Response maximumflow(@PathParam("source") long source,
                                @PathParam("sink")   long sink) {
        JSONObject obj = new org.json.JSONObject();
        obj.put("source-id",source);
        obj.put("sink-id",  sink);
        obj.put("maxflow",  this.getMaxFlow(source,sink));
        return Response.ok(obj.toString(), MediaType.APPLICATION_JSON)
        .header("X-Stream", "true")
        .build();
    }
    /*
     * Looks for all the found paths and assings them an unique temp property
     * to calculate the maxflow.
     */
    private void assingTempProperty(UUID uuid){
        TraversalDescription tempTraversal  = this.database.traversalDescription()
        .depthFirst()
        .uniqueness(Uniqueness.RELATIONSHIP_GLOBAL);
        String uuidString = "flw-"+uuid.toString();
        Transaction tx = database.beginTx();
        try {
            for(Relationship r : tempTraversal.traverse(this.nSource)
                .relationships()){
                if (r.hasProperty("weight"))
                    r.setProperty(uuidString,Double.parseDouble((String)r.getProperty("weight")) * 1.0);
            }
            tx.success();
        } catch (Exception e) {
            System.err.println("MaximumFlow.assingTempProperty: " + e);
            tx.failure();
        } finally {
            tx.close();
        }
    }
    /*
     * Looks for all the found paths and removes the temp Property
     */
    private void removeTempProperty(final UUID uuid){
        TraversalDescription tempTraversal  = this.database.traversalDescription()
        .depthFirst()
        .uniqueness(Uniqueness.RELATIONSHIP_GLOBAL);
        String uuidString = "flw-"+uuid.toString();
        Transaction tx = database.beginTx();
        try {
            for(Relationship r : tempTraversal.traverse(this.nSource)
                .relationships()){
                if (r.hasProperty("weight"))
                    r.removeProperty(uuidString);
            }
            tx.success();
        } catch (Exception e) {
            System.err.println("MaximumFlow.removeTempProperty: " + e);
            tx.failure();
        } finally {
            tx.close();
        }
    }
    /*
     * Calculates the MaximumFlow given a source and a sink.
     * ** Please note that in order to avoid infinite augmenting path search
     *    we have defined a max depth through the path.
     *    This means, the more depth, the more accurate, but more expensive ***
     * @param source    the source node id
     * @param sink      the sink node id
     * @return double   as the maximum flow value.
     */
    public double getMaxFlow(long source, long sink){
        double flow = 0.0;
        double accumulator = 0.0;
        double min = Double.MAX_VALUE;
        this.uuid  = UUID.randomUUID(); // Process uniqueness...
        String flowUUID = "flw-" + this.uuid.toString();
        Transaction tx = database.beginTx();
        try {
            this.nSink   = database.getNodeById(sink);
            this.nSource = database.getNodeById(source);
            //Assing an unique temp property to relationships...
            this.assingTempProperty(this.uuid);
            //Search for the maximum flow between a source and sink...
            for(org.neo4j.graphdb.Path p : this.database.traversalDescription()
                .depthFirst()
                .evaluator(Evaluators.endNodeIs(Evaluation.INCLUDE_AND_CONTINUE,
                                                Evaluation.EXCLUDE_AND_CONTINUE,
                                                this.nSink))
                .relationships(Rels.HAS_TERM)
                /* The next line makes a mess,
                 * :::Theory:::
                 * If you have a sparsely connected graph, and apply
                 * relationship-uniqueness, you have to explore very very long
                 * paths to really find all the unique relationships in your
                 * graph, so you have to meander back and forth until you find
                 * the last globally unique relationship.
                 * Thanks @Michael Hunger (Neo4j Team).
                 *
                 * The solution is to write a custom Uniqueness overriding
                 * the method.
                 * For a future version please check: http://goo.gl/f8EP4U
                 */
                .uniqueness(Uniqueness.NODE_PATH)
                /*
                 * Let's solve (by now) the issue by just adjusting the depth.
                 */
                .evaluator(Evaluators.toDepth(5))
                .traverse(this.nSource)) {
                //Getting the min value from each path...
                for (Relationship r : p.relationships()) {
                    if (r.hasProperty(flowUUID) && (Double)r.getProperty(flowUUID) < min)
                        flow = (Double)r.getProperty(flowUUID) * 1.0;
                }
                accumulator = accumulator + flow;
                // Setting up the flow values for the next round...
                for (Relationship r: p.relationships()){
                    if (r.hasProperty(flowUUID))
                        r.setProperty(flowUUID,(Double)r.getProperty(flowUUID)-flow);
                }
                // Save the flow in the node...
                for(Node n : p.nodes()){
                    if((n!= p.startNode()) && (n!=p.endNode())){
                        if(n.hasProperty("flow")) {
                            n.setProperty("flow",(Double)n.getProperty("flow")+ Math.round(Math.abs(flow)*100.0)/100.0);
                        } else {
                            n.setProperty("flow", Math.round(Math.abs(flow)*100.0)/100.0);
                        }
                    }
                }
            }
            //Remove the temp property...
            this.removeTempProperty(this.uuid);
            //Save the maxflow between these nodes.
            nSource.createRelationshipTo(nSink,Rels.MAX_FLOW)
            .setProperty("maxflow", accumulator);
            tx.success();
        } catch (Exception e) {
            System.err.println("hintplugin.utils.MaximumFlow.getMaxFlow: " + e);
            tx.failure();
        } finally {
            tx.close();
        }
        return Math.round(Math.abs(accumulator)*100.0)/100.0;
    }
    /*
     * Returns the node flow through a node (Deprecated)
     */
    @Deprecated
    public double getTargetNodeFlow(){
        return this.targetNodeFlow;
    }
}