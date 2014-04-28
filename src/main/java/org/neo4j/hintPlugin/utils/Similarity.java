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
package org.neo4j.hintPlugin.utils;

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

    public Similarity( @Context GraphDatabaseService database ) {
        this.database = database;
    }
    @GET
    @Produces( MediaType.TEXT_PLAIN )
    @Path( "/{nodeA}/{nodeB}" )
    public Response hello( @PathParam( "nodeA" ) long nodeA, @PathParam( "nodeB" ) long nodeB)
    {
        // Do stuff with the database
    /*
        Node node_a = database.getNodeById(nodeA);
        Node node_b = database.getNodeById(nodeB);
    */
        return Response.status( Status.OK ).entity(
                ("Similarity:"+this.getSimilarity()).getBytes( Charset.forName("UTF-8") ) ).build();
    }
    
    private int getSimilarity(){
        return 5;
    }
}