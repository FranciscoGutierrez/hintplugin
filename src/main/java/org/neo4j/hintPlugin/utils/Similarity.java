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

@Path( "/similarity" )
public class Similarity {
    
    private final GraphDatabaseService database;
    private Node node_a;
    private Node node_b;

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
                ("Similarity:"+this.getSimilarity(node_a, node_b)).getBytes(Charset.forName("UTF-8"))).build();
    }
    
    private long getSimilarity(long node_a, long node_b){
/*      this.node_a = this.database.getNodeById(node_a);
        this.node_b = this.database.getNodeById(node_b); */
        System.out.println("************* The server has processed: " + (node_a + node_b));
        
        return 5;
    }
}