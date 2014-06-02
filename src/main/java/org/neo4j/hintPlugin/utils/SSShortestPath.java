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

import java.nio.charset.Charset;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipExpander;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.kernel.Traversal;
import org.neo4j.tooling.GlobalGraphOperations;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
/**
 * SSShortest Path Class
 * Returns and sets up the shortest path value in the Neo4j Graph.
 * @author  Francisco Gutiérrez. (fsalvador23@gmail.com)
 * @proyect Reaumobile (Universidad de las Américas Puebla Team)  http://ict.udlap.mx/
 * @version 0.1
 * @since 2014-05-01
 */

@Path("/ssshortestpath")
public class SSShortestPath {
    private final GraphDatabaseService database;
    private int traversedNodes;
    private String computeStatus;
    private double computedSeconds;
    private Node nSource;
    private Node nTarget;
    /**
     *  The relationship types that are allowed for this class...
     */
    private enum Rels implements RelationshipType {
        SS_SPATH, KNOWS
    }
    /**
     * The Public Constructor of this class.
     * @param database: The GraphDatabaseService object needed to feed this class...
     */
    public SSShortestPath(@Context GraphDatabaseService database) {
        this.database = database;
    }
    /**
     * RESTFUL Single Source Shortest Path Service
     * @param target the target node ID
     * @return double as the betweenness value.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{targetA}/{targetB}")
    public Response betweeness(@PathParam("targetA") long nodeTargetA,
                               @PathParam("targetB") long nodeTargetB) {
        Gson       gson = new GsonBuilder().create();
        JsonObject obj  = new JsonObject();
        try{
            obj.addProperty("shortestPaths", this.setSSShortestPaths(nodeTargetA,nodeTargetB));
        } catch (Exception ex) {
            System.err.println("utils.SSShortestPath Class: " + ex);
        }
        return Response.ok(gson.toJson(obj), MediaType.APPLICATION_JSON).build();
    }
    /**
     * Calculates the Single Source Shortespath
     * @param source    the source node id
     * @param target    the target node id
     * @return int as the number of shortests paths between those nodes.
     */
    public double setSSShortestPaths(long sourceId, long targetId){
        RelationshipExpander expander = Traversal.expanderForTypes(Rels.KNOWS, Direction.BOTH);
        PathFinder <org.neo4j.graphdb.Path> shortestPathFinder = GraphAlgoFactory.shortestPath(expander, 1000,10);
        double shortestpaths = 0.0;
        Transaction tx = database.beginTx();
        try {
            this.nSource = database.getNodeById(sourceId);
            this.nTarget = database.getNodeById(targetId);
            for(org.neo4j.graphdb.Path p : shortestPathFinder.findAllPaths(this.nSource,this.nTarget)) {
                for(Node n : p.nodes()) {
                    if((n!= p.startNode()) && (n!=p.endNode()))
                         if(n.hasProperty("ssspaththrough"))
                             n.setProperty("ssspaththrough", (Double)n.getProperty("ssspaththrough")+1);
                }
                shortestpaths++;
            }
            this.nSource.createRelationshipTo(this.nTarget,Rels.SS_SPATH)
            .setProperty("shortestpaths", shortestpaths);
            tx.success();
        } catch (Exception e) {
            System.err.println("hintplugin.utils.SSShortestPath: " + e);
            tx.failure();
        } finally {
            tx.close();
        }
        return shortestpaths;
    }
    
    /**
     * Field Variable Wrappers, just for privacy sake...
     * Under development
     */
    private int getTraversedNodes(){
        return this.traversedNodes;
    }
    private double getComputedSeconds() {
        return this.computedSeconds;
    }
    private String getComputeStatus() {
        return this.computeStatus;
    }
}