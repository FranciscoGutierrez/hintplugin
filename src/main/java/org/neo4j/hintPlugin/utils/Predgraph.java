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

@Path("/predgraph")
public class Predgraph {
    
    private final GraphDatabaseService database;
    private Node node;
    private double numberOfWords;
    /*
     * The Public constructor.
     */
    public Predgraph(@Context GraphDatabaseService database) {
        this.database = database;
    }
    /*
     * The RESTful Method to be called to retrieve Similarity Between two nodes.
     * @param node: the target node.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{node}")
    public Response predgraph(@PathParam("node") long node) {
        Gson       gson = new GsonBuilder().create();
        JsonObject obj  = new JsonObject();
        try{
            obj.addProperty("predgraph",    this.getPredgraph(node));
            obj.addProperty("nodeId",       node);
            obj.addProperty("numberOfWords",this.numberOfWords);
        } catch (Exception ex) {
            System.err.println("utils.PredGraph Class: " + ex);
        }
        return Response.ok(gson.toJson(obj), MediaType.APPLICATION_JSON).build();
    }
    /*
     * Calculates Similarity Between Two Nodes, Based on Jaccard Index.
     * @param node_a:       the start node to calculate similarity.
     * @param node_b:       the end node to calculate similarity.
     * @param threshold:    the threshold that must be equal or up to create a relationship.
     */
    private double getPredgraph(long node){
        this.numberOfWords  = 0.0;
        double predgraph    = 0.0;
        double degree       = 0.0;
        Transaction tx = database.beginTx();
        try {
            this.node = database.getNodeById(node);
            for (Relationship r: this.node.getRelationships()){
                if(r.hasProperty("weight")){
                    predgraph = new Double(r.getProperty("weight").toString()) + predgraph;
                    degree++;
                }
            }
            this.numberOfWords = degree;
            this.node.setProperty("predgraph",predgraph);
            tx.success();
        } catch (Exception e) {
            System.out.println("Fail, This happened: " + e);
        } finally {
            tx.close();
        }
        return predgraph;
    }
}