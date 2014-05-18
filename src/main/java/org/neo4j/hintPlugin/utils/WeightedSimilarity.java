/**
 * Licensed to Neo Technology under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo Technology licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.neo4j.hintplugin.utils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;

import java.lang.Math;

@Path("/weightedsimilarity")
public class WeightedSimilarity {
    
    private final GraphDatabaseService database;
    private Node node_a;
    private Node node_b;
    private final double threshold = 0.5;
    
    enum MyRelationshipTypes implements RelationshipType {
        KNOWS, IS_SIMILAR
    }
    /*
     * The Public constructor.
     */
    public WeightedSimilarity(@Context GraphDatabaseService database) {
        this.database = database;
    }
    /*
     * The RESTful Method to be called to retrieve Similarity Between two nodes.
     * @param node_a:
     * @param node_b:
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{node_a}/{node_b}")
    public Response weightedSimilarity(@PathParam("node_a") long node_a,
                                       @PathParam("node_b") long node_b) {
        Gson       gson = new GsonBuilder().create();
        JsonObject obj  = new JsonObject();
        try{
            obj.addProperty("weighted-similarity",  this.getWeightedSimilarity(node_a, node_b));
            obj.addProperty("node-start",      node_a);
            obj.addProperty("node-end",      node_b);
            obj.addProperty("threshold",   this.threshold);
        } catch (Exception ex) {
            System.err.println("utils.Similarity Class: " + ex);
        }
        return Response.ok(gson.toJson(obj), MediaType.APPLICATION_JSON).build();
    }
    /*
     * Calculates Similarity Between Two Nodes, Based on Jaccard Index.
     * @param node_a:       the start node to calculate similarity.
     * @param node_b:       the end node to calculate similarity.
     * @param threshold:    the threshold that must be equal or up to create a relationship.
     */
    private double getWeightedSimilarity(long node_a, long node_b){
        double similarity           = 0.0;
        double node_intersection    = 0.0;
        double node_union           = 0.0;
        double count_node_a         = 0.0;
        double count_node_b         = 0.0;
        double count_intersection   = 0.0;
        Transaction tx = database.beginTx();
        try {
            this.node_a = database.getNodeById(node_a);
            this.node_b = database.getNodeById(node_b);
            //Union;
            for (Relationship a: this.node_a.getRelationships()) {
                node_union = new Double(a.getProperty("weight").toString()) + node_union;
                count_node_a++;
            }
            for (Relationship b: this.node_b.getRelationships()) {
                node_union = new Double(b.getProperty("weight").toString()) + node_union;
                count_node_b++;
            }
            //Intersection;
            for (Relationship a: this.node_a.getRelationships()) {
                for (Relationship b: this.node_b.getRelationships()) {
                    if(a.hasProperty("weight") && b.hasProperty("weight")){
                        if(a.getEndNode().getId() == b.getEndNode().getId()){
                            node_intersection = new Double(a.getProperty("weight").toString()) +
                            new Double(b.getProperty("weight").toString()) + node_intersection;
                            count_intersection++;
                        }
                    }
                }
            }
            //Jaccard Index...
            node_intersection = node_intersection/count_intersection;
            node_union = node_union/(count_node_a + count_node_b);
            System.out.println("****** Inter:" + node_intersection);
            System.out.println("****** Union:" + node_union);
            if (Math.abs(node_union) >= 0) {
                similarity = Math.abs(node_intersection)/Math.abs(node_union);
            } else {
                similarity = 1.0;
            }
            System.out.println("******" + similarity);
            //Destroy any "similarity" relationships... (if any)
            for (Relationship r: this.node_a.getRelationships(MyRelationshipTypes.IS_SIMILAR)){
                r.delete();
            }
            if(similarity >= 0.5){
                Relationship rs = this.node_a.createRelationshipTo(this.node_b, MyRelationshipTypes.IS_SIMILAR);
                rs.setProperty("similarity", similarity);
            }
            tx.success();
        } catch (Exception e) {
            System.out.println("Fail, This happened: " + e);
        } finally {
            tx.close();
        }
        return similarity;
    }
}