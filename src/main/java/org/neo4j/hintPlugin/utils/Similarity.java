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

import java.nio.charset.Charset;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.json.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import java.lang.Math;
@Path("/similarity")
public class Similarity {
    
    private final GraphDatabaseService database;
    private Node node_a;
    private Node node_b;
    private double threshold;
    
    enum MyRelationshipTypes implements RelationshipType {
        KNOWS, IS_SIMILAR
    }
    /*
     * The Public constructor.
     */
    public Similarity(@Context GraphDatabaseService database) {
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
    public Response similarity(@PathParam("node_a") long node_a, @PathParam("node_b") long node_b) {
        JSONObject obj = new org.json.JSONObject();
        try{
            obj.put("similarity",  this.getSimilarity(node_a, node_b));
            obj.put("node-a",      node_a);
            obj.put("node-b",      node_b);
            obj.put("threshold",   0.5);
        } catch (Exception ex) {
            System.err.println("utils.Similarity Class: " + ex);
        }
        return Response.ok(obj.toString(), MediaType.APPLICATION_JSON).build();
    }
    /*
     * Calculates Similarity Between Two Nodes, Based on Jaccard Index.
     * @param node_a:       the start node to calculate similarity.
     * @param node_b:       the end node to calculate similarity.
     * @param threshold:    the threshold that must be equal or up to create a relationship.
     */
    private double getSimilarity(long node_a, long node_b){
        double node_a_degree        = 0.0;
        double node_b_degree        = 0.0;
        double similarity           = 0.0;
        double node_intersection    = 0.0;
        double node_union           = 0.0;
        Transaction tx = database.beginTx();
        try {
            this.node_a = database.getNodeById(node_a);
            this.node_b = database.getNodeById(node_b);
            Iterable<Relationship> relationships_a = this.node_a.getRelationships();
            Iterable<Relationship> relationships_b = this.node_b.getRelationships();
            node_a_degree = IteratorUtil.count(relationships_a);
            node_b_degree = IteratorUtil.count(relationships_b);
            node_union = node_a_degree + node_b_degree;
            for (Relationship a: this.node_a.getRelationships()) {
                for (Relationship b: this.node_b.getRelationships()) {
                    if(a.getEndNode().getId() == b.getEndNode().getId())
                        node_intersection++;
                }
            }
            if ( (node_intersection == 0) && (node_union <=0)) {
                similarity = 1.0;
            } else {
                similarity = Math.abs(node_intersection)/Math.abs(node_union);
            }
            
            if(similarity >= 0.5){
                this.node_a.createRelationshipTo(this.node_b, MyRelationshipTypes.IS_SIMILAR);
                System.out.println("*************   Warning: Relationship Created ");
            }
            System.out.println("*************        Union: " + node_union);
            System.out.println("************* Intersection: " + node_intersection);
            System.out.println("*************   Similarity: " + similarity);
            tx.success();
        } catch (Exception e) {
            System.out.println("Fail, This happened: " + e);
        } finally {
            tx.close();
        }
        return similarity;
    }
}