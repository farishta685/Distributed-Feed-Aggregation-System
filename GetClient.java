import java.net.*;
import java.util.UUID;
import java.io.*;

public class GetClient {

    private static Socket gcSocket;
    private static BufferedReader readResponse;
    private static PrintWriter writeRequests;
    public static LamportClockGC myLC;

    // constructor will initialise socket and variables to read and write requests
    public GetClient(int port)
    {
        try {
            gcSocket = new Socket("localhost", port);
            readResponse = new BufferedReader(new InputStreamReader(gcSocket.getInputStream()));
            writeRequests = new PrintWriter(gcSocket.getOutputStream());
            myLC = new LamportClockGC();

        } catch (Exception e) {
        //    System.out.println("16- client constructor error: "+e);
        }
    }

    // creates a new get request which just contains GET .... and the request ID
    public static void sendGETrequest(int attempt)
    {  
        try {
            writeRequests.println("GET /aggregationFeed.txt HTTP/1.11 ");
            myLC.incrementLC();
            writeRequests.println(myLC.getLC());
            UUID id = UUID.randomUUID();
            String RequestID = id.toString();
            writeRequests.println("ReqID: "+ RequestID);
            writeRequests.flush();
            readRequest(attempt);
            
        } catch (Exception e) {
            // System.out.println("35- some client sendGET error:  "+e);
        }
    }

    // retries 3 times 
    public static void retryagain(int attempt)
    {
        if(attempt<3)
        {
            sendGETrequest(attempt+1);

        }
    }


    // reads the response from server whihc includes the aggregation feed 
    public static void readRequest(int attempt)
    {
        try {
            String reply = readResponse.readLine();
            // if reply is 200 or 201 means feed will be sent so we can print 
            if (reply.startsWith("HTTP/20"))
            {
                int servLc = Integer.parseInt(readResponse.readLine());
                myLC.updateLC(servLc);
                reply = readResponse.readLine();
                while(readResponse.ready())
                { 
                    reply = readResponse.readLine();
                    if(reply.equals("<feed>"))
                    {
                        reply = readResponse.readLine();
                        while(!(reply.equals("</feed>")))
                        {
                            reply = readResponse.readLine();
                            System.out.println(reply);
                        }   
                    }
                }
            }
            // else if server is dead and never returned any response, then you just ping the server 
            else
            {
                retryagain(attempt);
            }

        } catch (Exception e) {
            // System.out.println("115- read response error... "+e);
        }
    }

    // create a new content server and sends get request!
    public static void main(String[] args)
    {
        try {
            int port = Integer.parseInt(args[0]);
            GetClient gcm = new GetClient(port);
            gcm.sendGETrequest(0);
            
        } catch (Exception e) {
            // System.out.println("77- Get client main error... "+e);
        }
    } 
}


class LamportClockGC
{
    public int myLC;
    
    public LamportClockGC()
    {
        myLC = 0;
    }
    public int getLC()
    {
        return myLC;
    }

    public void incrementLC()
    {
        myLC += 1;
    }
    public int updateLC(int requestLC)
    {
        if( myLC >= requestLC)
        {
            myLC = myLC +1; 
            return myLC;
        }
        else {
            myLC = requestLC+1;
            return myLC;
        }
        
    }
}