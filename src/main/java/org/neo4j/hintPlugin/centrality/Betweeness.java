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
package org.neo4j.hintplugin.centrality;

import java.nio.charset.Charset;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import static org.neo4j.graphalgo.GraphAlgoFactory.allPaths;
import static org.neo4j.kernel.Traversal.expanderForAllTypes;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Betweeness Class
 * Returns the Maximum flow value within a graph given a source and sink nodes.
 * @author  Francisco Gutiérrez.
 * @version 0.1
 * @since 2014-05-01
 */

@Path("/betweeness")
public class Betweeness {

    private final GraphDatabaseService database;
    private Node nSource;
    private Node nSink;

    enum MyRelationshipTypes implements RelationshipType
    {
        KNOWS, IS_SIMILAR
    }

    public Betweeness( @Context GraphDatabaseService database ) {
        this.database = database;
    }
    
    @GET
    @Produces( MediaType.TEXT_PLAIN )
    @Path( "/{source}/{sink}" )
    public Response maximumflow(@PathParam("source") long source, @PathParam("sink") long sink) {
        String json = "{\"betweeness\":" + this.getFlow(source, sink) + "}";
        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }
    
    /*
     * Calculates the MaximumFlow given a source and sink nodes.
     *
     * @param source    the source node id
     * @param sink      the sink node id
     * @return double   as the maximum flow value.
     */
    
    private double getFlow(long source, long sink){
        int maxDepth = 10000;
        int flow = 0;
        int accumulator = 0;
        
        List<Integer> flows = new ArrayList<Integer>();
        Transaction tx = database.beginTx();
        try {
            this.nSource = database.getNodeById(source);
            this.nSink = database.getNodeById(sink);
            
            for(org.neo4j.graphdb.Path p : allPaths(expanderForAllTypes(Direction.BOTH),maxDepth).findAllPaths(this.nSource,this.nSink)) {
                for (Relationship r : p.relationships()) {
                    flows.add((Integer)r.getProperty("weight"));
                }
                flow = Collections.min(flows);
                flows.clear();
                accumulator = accumulator + flow;
                for (Relationship r: p.relationships()){
                    r.setProperty("weight",(Integer)r.getProperty("weight") - flow);
                }
            }
            System.out.println("*********         Accumulator: " + accumulator);
            tx.success();
        } catch (Exception e) {
            System.out.println("********* Fail, This happened: " + e);
            tx.failure();
        } finally {
             tx.close();
        }
        return accumulator;
    }
}