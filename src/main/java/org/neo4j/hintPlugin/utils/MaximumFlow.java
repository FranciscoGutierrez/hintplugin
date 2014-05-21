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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Direction;
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
    private final int maxDepth = 100; //10,000 As Default ** Notice.
    private Node nSource;
    private Node nSink;
    private double targetNodeFlow;
    private Map <Long, Double> nodeFlows;
    private PathFinder <org.neo4j.graphdb.Path> simplePaths;
    private Expander expander;
    private Iterable <org.neo4j.graphdb.Path> allFoundPaths;
    private UUID uuid;
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
            obj.put("maxflow",      this.getMaxFlow(source,sink,target));
            obj.put("target-flow",  this.getTargetNodeFlow());
            tx.success();
        } catch (Exception ex) {
            System.err.println("MaximumFlowService: " + ex);
            tx.failure();
        } finally {
            tx.close();
        }
        return Response.ok(obj.toString(), MediaType.APPLICATION_JSON).build();
    }
    /*
     * Looks for all the found paths and assings them an unique temp property
     * to calculate the maxflow.
     */
    private void assingTempProperty(UUID uuid){
        for(org.neo4j.graphdb.Path p : this.allFoundPaths) {
            for (Relationship r : p.relationships()) {
                r.setProperty("flw-"+uuid.toString(),r.getProperty("weight"));
            }
        }
    }
    /*
     * Looks for all the found paths and removes the temp Property
     */
    private void removeTempProperty(final UUID uuid){
        final Iterable <org.neo4j.graphdb.Path> innerPaths = this.allFoundPaths;
        Thread thread = new Thread("removeTmpFlw: "+uuid.toString()) {
            public void run(){
                Transaction tx = database.beginTx();
                try {
                    for(org.neo4j.graphdb.Path p : innerPaths) {
                        for (Relationship r : p.relationships()) {
                            r.removeProperty("flw-"+uuid.toString());
                        }
                    }     tx.success();
                } catch (Exception e) {
                    System.err.println("MaximumFlow.removeFlow: " + e);
                    tx.failure();
                } finally {
                    tx.close();
                }
                
            }
        };
        thread.start();
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
        nodeFlows = new HashMap<Long, Double>();
        this.uuid = UUID.randomUUID(); //Process uniqueness...
        //Declare a simple list to store the flow and get the min.
        List<Double> flows = new ArrayList<Double>();
        Transaction tx = database.beginTx();
        try {
            this.nSink   = database.getNodeById(sink);
            this.nSource = database.getNodeById(source);
            //this.allFoundPaths = simplePaths.findAllPaths(this.nSource,this.nSink);
            //Assing an unique temp property.
            this.assingTempProperty(this.uuid);
            //Search for the maximum flow between the source and sink...
            for(org.neo4j.graphdb.Path p : this.allFoundPaths) {
                //Look for all the properties and put them into a List
                for (Relationship r : p.relationships()) {
                    flows.add((Double)r.getProperty("flw-"+this.uuid.toString()));
                }
                /*
                 * Calculate the min, because:
                 * flow = Collections.min(flows);
                 * Doesn't work with double primitives...
                 */
                for (double ds : flows) {
                    if (ds < min ) flow=ds;
                }
                // Look for the flow that passes for desired Node
                flows.clear();
                accumulator = accumulator + flow;
                // Setup the flow values for the next round...
                for (Relationship r: p.relationships()){
                    r.setProperty("flw-"+this.uuid.toString(),(Double)r.getProperty("flw-"+ this.uuid.toString()) - flow);
                }
                // Save the flow...
                for(Node n : p.nodes()){
                    if( (n!= p.startNode()) && (n!=p.endNode()) )
                        if(n.getId() == target)
                            targetNodeFlow = flow;
                }
            }
            //Remove the temp property... *** Please send me in a thread!!!
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
     *
     * @param source    the source node id
     * @param sink      the sink node id
     * @return double   as the maximum flow value.
     */
    public double getMaxFlow(long source, long sink){
        double flow = 0.0;
        double accumulator = 0.0;
        double min = Double.MAX_VALUE;
        nodeFlows = new HashMap<Long, Double>();
        String uuid = UUID.randomUUID().toString(); //For sake of process uniqueness
        //Declare a simple list to store the flow and get the min.
        List<Double> flows = new ArrayList<Double>();
        Transaction tx = database.beginTx();
        try {
            this.nSource = database.getNodeById(source);
            this.nSink = database.getNodeById(sink);
            //Assing an unique temp property.
            for(org.neo4j.graphdb.Path p : allSimplePaths(expanderForAllTypes(Direction.BOTH),maxDepth)
                .findAllPaths(this.nSource,this.nSink)) {
                for (Relationship r : p.relationships()) {
                    r.setProperty("flw-"+uuid,r.getProperty("weight"));
                }
            }
            //Search for the maximum flow between the source and sink...
            for(org.neo4j.graphdb.Path p : allSimplePaths(expanderForAllTypes(Direction.BOTH),maxDepth).findAllPaths(this.nSource,this.nSink)) {
                //Look for all the properties and put them into a List
                for (Relationship r : p.relationships()) {
                    flows.add((Double)r.getProperty("flw-"+uuid));
                }
                /*
                 * Calculate the min, because:
                 * flow = Collections.min(flows);
                 * Doesn't work with double primitives...
                 */
                for (double ds : flows) {
                    if (ds < min ) flow=ds;
                }
                // Look for the flow that passes for desired Node
                flows.clear();
                accumulator = accumulator + flow;
                // Setup the flow values for the next round...
                for (Relationship r: p.relationships()){
                    r.setProperty("flw-"+uuid,(Double)r.getProperty("flw-"+ uuid.toString()) - flow);
                }
            }
            //Remove the temp property...
            for(org.neo4j.graphdb.Path p : allSimplePaths(expanderForAllTypes(Direction.BOTH),maxDepth).findAllPaths(this.nSource,this.nSink)) {
                for (Relationship r : p.relationships()) {
                    r.removeProperty("flw-"+uuid);
                }
            }
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