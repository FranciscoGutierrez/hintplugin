HintPlugin v0.1
==========
An unmanaged extension that enables Neo4j (2.0.1) to give recommeder system functionality.

Setting everything up!
----------------------
From terminal use Maven to build the project:
```
mvn clean package
```  
Copy the generated **JAR File** at...
```
hintplugin/target/
```
...into **Neo4j Plugins Directory**.  
```
neo4j/plugins/  
```
**Warning:** Before continue stop the Neo4j Instance!  
Then, in **conf/neo4j-server.properties** copy/paste the following:  
```  
org.neo4j.server.thirdparty_jaxrs_classes=org.neo4j.hintplugin.utils=/hintplugin/utils  
```  
**Start** Neo4j Sever...  
Have Fun!


What can I do?
--------------
- Similarity between nodes.
- Freeman Centralities.
- Freeman Flow-Centralities.

**Warning...** Still under development...