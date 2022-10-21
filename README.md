# Distributed-Feed-Aggregation-System
This project was implemented for an Assignment for the course Distributed Systems. 
In this Distributed system there are 3 entities:
* Aggregation Server: listens for connections and uses Threads to read and process client and content server requests. Aggregates News Feed sent by all the content servers into an aggregation feed file
* Content server: makes conection to Aggregation server and sends a PUT request containing an Atom feed to the server to upload on the Aggregation feed
* Get Client: Makes connection to the server and sends a GET request to ask for the aggregation feed
* Lamport clock class: is a class which is used by all the entities above to synchronise and for the Aggregation server to keep records of time stamps of each request and response


# To compile the code, run the following commands:
* javac AggregatedServer.java
* javac ContentServer.java
* javac GetClient.java


# To run the code, run the following commands in seperate terminal windows:
* java AggregatedServer
* java ContentServer
* java GetClient
