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
import static org.neo4j.graphalgo.GraphAlgoFactory.allSimplePaths;
import static org.neo4j.kernel.Traversal.expanderForAllTypes;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Expander;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.helpers.collection.IteratorUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;
import java.util.UUID;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

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
    private final int maxDepth = 1000; //10,000 As Default ** Notice.
    private Node nSource;
    private Node nSink;
    private double targetNodeFlow;
    private Map <Long, Double> nodeFlows;
    private PathFinder <org.neo4j.graphdb.Path> simplePaths;
    private Expander expander;
    private Iterable <org.neo4j.graphdb.Path> allFoundPaths;
    private UUID uuid;
    
    private enum Rels implements RelationshipType {
        CONTAINED_IN, KNOWS
    }
    
    /*
     * The Public Constructor of this class.
     * @param database: The GraphDatabaseService object needed to feed this class...
     */
    public MaximumFlow(@Context GraphDatabaseService database) {
        this.database = database;
        this.expander = expanderForAllTypes(Direction.BOTH);
        this.simplePaths = allSimplePaths(expander,maxDepth);
    }
    /*
     * Maximum Flow: RESTful Service...
     * @param source: the id of the source node.
     * @param   sink: the id of the sink node.
     * @param target: the id of the target node to get the flow running through it.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path( "/{source}/{sink}/{target}" )
    public Response maximumflow(@PathParam("source") long source,
                                @PathParam("sink")   long sink,
                                @PathParam("target") long target) {
        JSONObject obj = new org.json.JSONObject();
        Transaction tx = database.beginTx();
        try {
            this.nSink   = database.getNodeById(sink);
            this.nSource = database.getNodeById(source);
            this.allFoundPaths = simplePaths.findAllPaths(this.nSource,this.nSink);
            obj.put("source-id",    source);
            obj.put("sink-id",      sink);
            obj.put("maxflow",      this.getMaxFlow(source,sink));
            obj.put("target-flow",  this.getTargetNodeFlow());
            tx.success();
        } catch (Exception ex) {
            System.err.println("MaximumFlowService: " + ex);
            tx.failure();
        } finally {
            tx.close();
        }
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
                    r.setProperty(uuidString,r.getProperty("weight"));
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
     * Calculates the MaximumFlow given a source, sink and a target.
     *
     * @param source    the source node id
     * @param sink      the sink node id
     * @param target    the target node to get the flow that is going through...
     * @return double   as the maximum flow value.
     */
    public double getMaxFlow(long source, long sink, long target){
        double flow = 0.0;
        double accumulator = 0.0;
        double min = Double.MAX_VALUE;
        this.uuid = UUID.randomUUID(); //Process uniqueness...
        String flowUUID = "flw-" + this.uuid.toString();
        Transaction tx = database.beginTx();
        try {
            this.nSink   = database.getNodeById(sink);
            this.nSource = database.getNodeById(source);
            if(this.allFoundPaths == null){
                this.allFoundPaths = simplePaths.findAllPaths(this.nSource,this.nSink);
            }
            //Assing an unique temp property to relationships...
            this.assingTempProperty(this.uuid);
            //Search for the maximum flow between a source and sink...
            for(org.neo4j.graphdb.Path p : this.allFoundPaths) {
                for (Relationship r : p.relationships()) {
                    if((Double)r.getProperty(flowUUID) < min)
                        flow = (Double)r.getProperty(flowUUID);
                }
                accumulator = accumulator + flow;
                // Set up the flow values for the next round...
                for (Relationship r: p.relationships()) {
                    r.setProperty(flowUUID,(Double)r.getProperty(flowUUID)-flow);
                }
                // Save the flow...
                for(Node n : p.nodes()) {
                    if((n!= p.startNode()) && (n!=p.endNode()))
                        if(n.getId() == target)
                            this.targetNodeFlow = flow;
                }
            }
            //Remove the temp property...
            this.removeTempProperty(this.uuid);
            tx.success();
        } catch (Exception e) {
            System.err.println("hintplugin.utils.MaximumFlow.getFlow: " + e);
            tx.failure();
        } finally {
            tx.close();
        }
        return accumulator;
    }
    /*
     * Calculates the MaximumFlow given a source and a sink.
     * **This Method DOES NOT save the Flow through a Target Node**
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
            if(this.allFoundPaths == null){
                this.allFoundPaths = simplePaths.findAllPaths(this.nSource,this.nSink);
            }
            //Assing an unique temp property to relationships...
            this.assingTempProperty(this.uuid);
            //Search for the maximum flow between a source and sink...
            for(org.neo4j.graphdb.Path p : this.database.traversalDescription()
                .depthFirst()
                .relationships(Rels.KNOWS, Direction.BOTH)
                .uniqueness(Uniqueness.NODE_PATH)
                .evaluator(Evaluators.excludeStartPosition())
                .evaluator(Evaluators.pruneWhereEndNodeIs(this.nSink))
                .traverse(this.nSource)){
                //System.out.println(p);
                for(Relationship r : this.database.traversalDescription()
                    .depthFirst()
                    .relationships(Rels.KNOWS, Direction.BOTH)
                    .uniqueness(Uniqueness.NODE_PATH)
                    .evaluator(Evaluators.excludeStartPosition())
                    .evaluator(Evaluators.pruneWhereEndNodeIs(this.nSink))
                    .traverse(this.nSource).relationships()){
                    
                    if (r.hasProperty(flowUUID))
                        if((Double)r.getProperty(flowUUID) < min)
                            flow = (Double)r.getProperty(flowUUID);
                }
                accumulator = accumulator + flow;
                // Set up the flow values for the next round...
                for(Relationship r : this.database.traversalDescription()
                    .depthFirst()
                    .relationships(Rels.KNOWS, Direction.BOTH)
                    .uniqueness(Uniqueness.NODE_PATH)
                    .evaluator(Evaluators.excludeStartPosition())
                    .evaluator(Evaluators.pruneWhereEndNodeIs(this.nSink))
                    .traverse(this.nSource).relationships()){
                    
                    if (r.hasProperty(flowUUID))
                        r.setProperty(flowUUID,(Double)r.getProperty(flowUUID)-flow);
                }
            }
            //Remove the temp property...
            this.removeTempProperty(this.uuid);
            tx.success();
        } catch (Exception e) {
            System.err.println("hintplugin.utils.MaximumFlow.getFlow: " + e);
            tx.failure();
        } finally {
            tx.close();
        }
        return accumulator;
    }
    
    public double getTargetNodeFlow(){
        return this.targetNodeFlow;
    }
}