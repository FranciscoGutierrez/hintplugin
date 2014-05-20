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

@Path("/similarity")
public class Similarity {
    
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
        Gson       gson = new GsonBuilder().create();
        JsonObject obj  = new JsonObject();
        try{
            obj.addProperty("similarity",  this.getSimilarity(node_a, node_b));
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
            System.out.println("****** Union:" + node_union);
            System.out.println("****** Inter:" + node_intersection);
            if (node_union != 0) {
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