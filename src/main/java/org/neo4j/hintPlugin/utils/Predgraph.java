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
        double predgraph    = 0.0;
        double degree       = 0.0;
        Transaction tx = database.beginTx();
        try {
            this.node = database.getNodeById(node);
            for (Relationship r: this.node.getRelationships()){
                predgraph = new Double(r.getProperty("weight").toString()) + predgraph;
                degree++;
            }
            this.numberOfWords = degree;
            tx.success();
        } catch (Exception e) {
            System.out.println("Fail, This happened: " + e);
        } finally {
            tx.close();
        }
        return predgraph;
    }
}