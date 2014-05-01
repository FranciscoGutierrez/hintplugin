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

import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;

@Path( "/similarity" )
public class Similarity {

    private final GraphDatabaseService database;
    private Node node_a;
    private Node node_b;

    enum MyRelationshipTypes implements RelationshipType
    {
        KNOWS, IS_SIMILAR
    }
    
    public Similarity( @Context GraphDatabaseService database ) {
        this.database = database;
    }
    
    @GET
    @Produces( MediaType.TEXT_PLAIN )
    @Path( "/{node_a}/{node_b}" )
    public Response similarity(@PathParam("node_a") long node_a, @PathParam("node_b") long node_b)
    {
        // Do stuff with the database
        return Response.status(Status.OK).entity(
                ("json {" + this.getSimilarity(node_a, node_b)).getBytes(Charset.forName("UTF-8"))).build();
    }
    
    private double getSimilarity(long node_a, long node_b){
        double similarity = 0.0f;
        Transaction tx = database.beginTx();
        try {
            this.node_a = database.getNodeById(node_a);
            this.node_b = database.getNodeById(node_b);
            
            Iterable<Relationship> relationships_a = this.node_a.getRelationships();
            Iterable<Relationship> relationships_b = this.node_b.getRelationships();
            
            int node_a_degree = IteratorUtil.count(relationships_a);
            int node_b_degree = IteratorUtil.count(relationships_b);
            
            double node_union = node_a_degree + node_b_degree;
            double node_intersection = 0.0f;
            
            for (Relationship a: this.node_a.getRelationships()){ //The starting node is a*
                for (Relationship b: this.node_b.getRelationships()){ //The starting node is b*
                    if(a.getEndNode().getId() == b.getEndNode().getId())
                        node_intersection++;
                }
            }
            similarity = (node_intersection)/(node_union - node_intersection);
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