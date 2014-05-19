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
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

@Path("/wsimilarity")
public class WSimilarity {
    
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
    public WSimilarity (@Context GraphDatabaseService database) {
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
        double euclideanDistance = 0.0;
        Gson       gson = new GsonBuilder().create();
        JsonObject obj  = new JsonObject();
        try{
            obj.addProperty("jaccardWSimilarity",  this.getJaccardSimilarity(node_a,node_b));
            obj.addProperty("euclideanWSimilarity",this.getEuclideanSimilarity(node_a,node_b));
            obj.addProperty("nodeStart",    node_a);
            obj.addProperty("nodeEnd",      node_b);
            obj.addProperty("threshold",    this.threshold);
        } catch (Exception ex) {
            System.err.println("utils.Similarity Class: " + ex);
        }
        return Response.ok(gson.toJson(obj), MediaType.APPLICATION_JSON).build();
    }
    /*
     * Calculates Similarity Between Two Nodes, Based on Euclidean Distance
     * @param node_a:       the start node to calculate similarity.
     * @param node_b:       the end node to calculate similarity.
     * @param threshold:    the threshold that must be equal or up to create a relationship.
     */
    private double getJaccardSimilarity(long node_a, long node_b){
        double similarity           = 0.0;
        double nodeUnion            = 0.0;
        double nodeIntersection     = 0.0;
        Transaction tx = database.beginTx();
        try {
            this.node_a = database.getNodeById(node_a);
            this.node_b = database.getNodeById(node_b);
            //Union;
            for (Relationship a: this.node_a.getRelationships()) {
                if(a.hasProperty("weight"))
                    nodeUnion = new Double(a.getProperty("weight").toString()) + nodeUnion;
            }
            for (Relationship b: this.node_b.getRelationships()) {
                if(b.hasProperty("weight"))
                    nodeUnion = new Double(b.getProperty("weight").toString()) + nodeUnion;
            }
            //Intersection;
            for (Relationship a: this.node_a.getRelationships()) {
                for (Relationship b: this.node_b.getRelationships()) {
                    if(a.hasProperty("weight") && b.hasProperty("weight")){
                        if(a.getEndNode().getId() == b.getEndNode().getId()){
                            nodeIntersection = new Double(a.getProperty("weight").toString()) +
                            new Double(b.getProperty("weight").toString()) + nodeIntersection;
                        }
                    }
                }
            }
            //Jaccard Index...
            if (Math.abs(nodeUnion) >= 0) {
                similarity = Math.abs(nodeIntersection)/Math.abs(nodeUnion);
            } else {
                similarity = 1.0;
            }
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
        return Math.round(similarity * 100.0)/100.0;
    }
    private double getEuclideanSimilarity(long nodeA, long nodeB){
        double similarity = 0.0;
        double[] array1 = null;
        double[] array2 = null;
        int nodeADegree = 0;
        int nodeBDegree = 0;
        int i = 0,j=0;
        Transaction tx = database.beginTx();
        try {
            this.node_a = database.getNodeById(nodeA);
            this.node_b = database.getNodeById(nodeB);
            for (Relationship a: this.node_a.getRelationships()) {
                nodeADegree++;
            }
            for (Relationship b: this.node_b.getRelationships()) {
                nodeBDegree++;
            }
            if(nodeADegree >= nodeBDegree){
                array1 = new double[nodeADegree];
                array2 = new double[nodeADegree];
            } else if (nodeADegree <= nodeBDegree) {
                array1 = new double[nodeBDegree];
                array2 = new double[nodeBDegree];
            }
            Arrays.fill(array1,0.0);
            Arrays.fill(array2,0.0);
            for (Relationship a: this.node_a.getRelationships()) {
                array1[i] = new Double(a.getProperty("weight").toString());
                i++;
            }
            for (Relationship b: this.node_b.getRelationships()) {
                array2[i] = new Double(b.getProperty("weight").toString());
                j++;
            }
            tx.success();
        } catch (Exception e) {
            System.out.println("Fail, This happened: " + e);
        } finally {
            tx.close();
        }
        return Math.round(Math.abs(1 - this.euclideanDistance(array1,array2))*100.0)/100.0;
    }
    private double euclideanDistance(double[] array1, double[] array2) {
        double sum = 0.0;
        for(int i=0; i<array1.length; i++) {
            sum = sum + Math.pow((array1[i]-array2[i]),2.0);
        }
        return Math.sqrt(sum);
    }
}