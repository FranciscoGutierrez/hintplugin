HintPlugin v0.1
==========
An unmanaged extension that enables Neo4j (2.0.1) to give recommender system functionality.

Please note that this extesion is intended to give a RESTful API support, however you can use this library through embedded mode.

Setting everything up!
----------------------
From terminal use Maven to build the project:
```bash
mvn clean package
```  
Copy the generated **JAR File** and paste it into neo4j plugins.
```
hintplugin/target/
```

Then, in **conf/neo4j-server.properties** copy/paste the following:  
```  
org.neo4j.server.thirdparty_jaxrs_classes=org.neo4j.hintplugin.utils=/hintplugin/utils  
```  
Go to your neo4j folder and restart neo4j server:
```bash
neo4j start
```
Have Fun!


What can I do?
--------------
- Similarity between nodes.
- Freeman Centralities.
- Freeman Flow-Centralities.

All the tests are based on Freeman Centrality graphs.
