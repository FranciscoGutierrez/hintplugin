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
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Path("/curlquery")
public class CurlQueries {
    
    private final GraphDatabaseService database;
    private Node node;
    /*
     * The Public constructor.
     */
    public CurlQueries (@Context GraphDatabaseService database) {
        this.database = database;
    }
    /*
     * The RESTful Method to generate cURL Queries
     * @param node_a:
     * @param node_b:
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response curlQueries() {
        Gson       gson = new GsonBuilder().create();
        JsonObject obj  = new JsonObject();
        int i = 0;
        try{
            this.getCurlQueriesForAllTheGraph();
        } catch (Exception ex) {
            System.err.println("utils.CurlQueries Class: " + ex);
        }
        return Response.ok(gson.toJson(obj), MediaType.APPLICATION_JSON).build();
    }
    /*
     * WARNING ** This Method retrieves curl maxflow queries for all the graph,
     *            handle carefully. **
     */
    private void getCurlQueriesForAllTheGraph() {
        String url = "curl -i -H \"Accept: application/json\" http://localhost:7474/hintplugin/utils/maximumflow/";
        List <Node> nodeArrayList = new ArrayList();
        Transaction tx = database.beginTx();
        try {
            Iterable <Node> allNodes = GlobalGraphOperations.at(this.database).getAllNodes();
            for(Node n1 : allNodes){
                nodeArrayList.add(n1);
            }
            for(Node n1 : nodeArrayList){
                for(Node n2 : nodeArrayList){
                    if (!(nodeArrayList.indexOf(n1) <= nodeArrayList.indexOf(n2))){
                        System.out.println(url + n1.getId() + "/" + n2.getId());
                    }
                }
            }
            tx.success();
        } catch (Exception e) {
            System.err.println("hintplugin.utils.MaximumFlow.getMaxFlow: " + e);
            tx.failure();
        } finally {
            tx.close();
        }
    }
}