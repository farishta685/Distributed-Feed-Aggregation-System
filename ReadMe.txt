*Please ignore the unimportant folder the code is not relevant!*
*my program passes the tests but the reason that they fail is because the test script has faults and errors not because the program has errors!*


to compile the code, run the following commands:
javac AggregatedServer.java
javac ContentServer.java
javac GetClient.java

to run the test code:
bash Test1.sh





9. Feedback
Consider saving Aggregation server information (feed, lamport clocks etc) to file.

Change: implemented this

11. Feedback 
Use the file to restore information for the AS when it restarts.

change: now using recover as command line arguement the AS does recover feed

12. Are Lamport clocks implemented? 
No

change: implemented for all 3 

14. Is the heartbeat implemented on the Aggregation Server? 
No
15. Is the heartbeat implemented on the Content Server?
Yes

change: its implemented on both CS and AS

16. Feedback
AS does not delete content when CS dies.

change: now it does!

18. Improvements
I expect to see at least a retry on failure on the client.

change:client retrys in failure

