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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.neo4j.helpers.collection.IteratorUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Betweeness Class
 * Returns the Betweenness Centrality given a target node.
 * @see (Brandes, U. (2001). A Faster Algorithm for Betweenness Centrality)
 * @author  Francisco Gutiérrez. (fsalvador23@gmail.com)
 * @author  Reaumobile, SNA Team (Universidad de las Américas Puebla). http://ict.udlap.mx/
 * @version 0.1
 * @since 2014-05-01
 */

@Path("/betweenness")
public class Betweenness {
    private final GraphDatabaseService database;
    /**
     * Relationships types allowed in this class.
     */
    private enum MyRelationshipTypes implements RelationshipType {
        KNOWS
    }
    /**
     * The constructor that passes the database service.
     */
    public Betweenness(@Context GraphDatabaseService database) {
        this.database = database;
    }
    /**
     * RESTFUL Betweenness Service
     * @param target the target node ID
     * @return double as the betweenness value.
     */
    @GET
    @Produces( MediaType.TEXT_PLAIN )
    @Path( "/{target}" )
    public Response betweeness(@PathParam("target") long targetNode) {
        Gson       gson = new GsonBuilder().create();
        JsonObject obj  = new JsonObject();
        try{
            obj.addProperty("betweenness", this.getBetweenness(targetNode));
            obj.addProperty("targetNode",  targetNode);
        } catch (Exception ex) {
            System.err.println("Centrality.Betweenness" + ex);
        }
        return Response.ok(gson.toJson(obj), MediaType.APPLICATION_JSON).build();
    }
    
    /**
     * Calculates Betweenness given a target node
     * @param target the target node ID
     * @return double as the betweenness value.
     */
    private double getBetweenness(long targetNodeId) {
       double betweeness = 0.0;/* 
        List shortestPathStack = new ArrayList();
        Transaction tx = database.beginTx();
        try {
            Node targetNode = database.getNodeById(targetNodeId);
            
        } catch (Exception e) {
           // GraphAlgoFactory.dijkstra().findAllPaths(nodea,nodeb)
        }*/
        /**
        int v, w;
		for(w = 0; w < size; w++)
			CB[w] = 0.0;
		for(int s = 0; s < size; s++){
			Pred.clear();
			// Initialization - Begin
			for(w = 0; w < size; w++){
				Pred.add(new ArrayList<Integer>());
				sigma[w] = 0;
				dist[w] = -1;
			}
			sigma[s] = 1;
			dist[s] = 0;
			Q.add(s);
			// Initialization - End
			while(!Q.isEmpty()){
				v = Q.poll();
				S.push(v);
				for(w = 0; w < size; w++)
					if(G[v][w] > 0){
						if(dist[w] < 0){
							Q.add(w);
							dist[w] = dist[v] + 1;
						}
						if(dist[w] == (dist[v] + 1)){
							sigma[w] = sigma[w] + sigma[v];
							Pred.get(w).add(v);
						}
					}
			}
			// Accumulation - Begin
			for(w = 0; w < size; w++)
				delta[w] = 0.0;
			while(!S.empty()){
				w = S.pop();
				for(int v1 : Pred.get(w))
					delta[v1] = delta[v1] + (sigma[v1] + 0.0)/sigma[w]*(1 + delta[w]);
				if(w != s)
					CB[w] = CB[w] + delta[w];
			}
			// Accumulation - End
		}
		for(v = 0; v < size; v++)
			System.out.println(v + " " + CB[v]);
         */
        return betweeness;
	}
}