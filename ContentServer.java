import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

public class ContentServer
{
    private  Socket csSocket;
    private  Socket hbSocket;
    public static boolean alive = true;
    private String myID;
    private static LamportClockCS mylampC;


    // content server constructor initialises two sockets one for heartbeat and 1 for the PUT request
    // also initialises the CS ID and the lamport clock
    public ContentServer(int port)
    {
        try {
            UUID id = UUID.randomUUID();
            myID = id.toString();
            csSocket = new Socket("127.0.0.1", port);
            hbSocket = new Socket("127.0.0.1", port);
            mylampC = new LamportClockCS();
            
        } catch (Exception e) {
            // System.out.println("\n31- contserv constructor error: "+e);
        }        
    }

    public static void main(String[] args)
    {
        try {
            // parses arguements for port number and name of atomfeed 
            // then starts a request and a hb thread
            String port_str = args[0];
            String atomFeedname = args[1];
            int port = Integer.parseInt(port_str);
            ContentServer csm = new ContentServer(port);
            Thread newhb = new initiate_heartbeat(csm.hbSocket, csm.myID);
            newhb.start();
            Thread newPut = new ProcessPutRequest(csm.csSocket, csm.myID, mylampC, atomFeedname);
            newPut.start();
            
            
        } catch (Exception e) {
            // System.out.println("126- content server main error: "+e);

        }
    }
}

// this thread processes the PUT requests 
class ProcessPutRequest extends Thread
{
    private Socket csSocket;
    private BufferedReader readResponse;
    private PrintWriter writeRequests;
    public static boolean alive = true;
    private static String myID;
    private static LamportClockCS serverLamp;
    private static String atomfeedname; 

    public ProcessPutRequest(Socket css, String id, LamportClockCS lc, String feedname)
    {
        try {
            myID = id;
            csSocket = css;
            readResponse = new BufferedReader(new InputStreamReader(csSocket.getInputStream()));
            writeRequests = new PrintWriter(csSocket.getOutputStream());
            serverLamp = lc;
            atomfeedname = feedname;

        } catch (Exception e) {
            // System.out.println("\n31- contserv constructor error: "+e);
        } 
    }

    // writes and send a new PUT request using a file given to the function
    // so far everytime the content server sends the same file to the aggregation server
    // with its own id and the unique id of the request
    public void writeNewPutRequest(File newFeed, String reqID)
    {
        try {
            writeRequests.println("PUT /atom.txt HTTP/1.11");
            serverLamp.incrementLC();
            writeRequests.println(Integer.toString(serverLamp.getLC())+"\n");
            writeRequests.println("myID: "+ myID);
            String requestID;
            if (reqID.equals("0"))
            {
                UUID id = UUID.randomUUID();
                requestID = id.toString();
                writeRequests.println("ReqID: "+requestID);
            }
            else{
                requestID = reqID;
                writeRequests.println("RedID: "+reqID);
            }
            writeRequests.println("User-Agent: ATOMClient/1/0");
            writeRequests.println("Author: Sleep_Deprived Programmer");
            writeRequests.println("Content-Type: text/html; charset=UTF-8");

            // now read from the given file and send each line to server
            BufferedReader myReader = new BufferedReader(new FileReader( newFeed));
            String nextline = myReader.readLine();
            while(nextline != null)
            {
                writeRequests.println(nextline);
                nextline = myReader.readLine();
            }
            writeRequests.flush();
            myReader.close();
            readResponse(newFeed, requestID);
            
        } catch (Exception e) {
            // System.out.println("\n71-write request error: "+e);
        }
    }

    // in case of not getting any response back, pings the server every 3 seconds
    // with the id of the recent request
    // if the id is in agfeed server will return HTTP/200
    // else it will ask to send it again so we will
    public void pingServer(File feedName, String reqID)
    {
        System.out.println("Server did not reply! trying to ping now");
        int ping_cout = 0;
        try {

            DataInputStream dis = new DataInputStream(csSocket.getInputStream());
            PrintStream ps = new PrintStream(csSocket.getOutputStream());
            while(true)
            {
                ps.println("Ping!");
                ps.println("reqID: "+ reqID);
                ps.flush();
                ping_cout +=1;
                String response = dis.readUTF();
                if (response.startsWith("HTTP/20"))
                {
                    System.out.println("server says:"+response);
                }
                else if(response.startsWith("Retry"))
                {
                    writeNewPutRequest(feedName, reqID);
                }
                Thread.sleep(3000);
                if(ping_cout==5)
                {
                    break;
                }
            }
        } catch (Exception e) {
            // System.out.println("71- Ping server Error... "+e);
        }
    }

    // fault tollerance mechanism for the content server
    // it checks the response sent by the server and behaves apporpriatly
    public void readResponse(File feedName, String reqID)
    {
        try {
            String reply = readResponse.readLine();

            // if reply is 200 or 201 it means feed aggregation was successful
            if (reply.startsWith("HTTP/20"))
            {
                int AsLampC = Integer.parseInt(readResponse.readLine());
                serverLamp.updateLC(AsLampC);
            } 
            // else if server is dead and never returned any response, then you just ping the server 
            else
            {
                pingServer(feedName, reqID);
            }

        } catch (Exception e) {
            // System.out.println("115- read response error... "+e);
        }
    }

    public void run()
    {
        try {
            File atomFeed = new File("test_Feeds/"+atomfeedname);
            
            UUID id = UUID.randomUUID();
            String requestID = id.toString();

            writeNewPutRequest(atomFeed, requestID);
            
        } catch (Exception e) {
            System.out.println("126- content server main error: "+e);

        }
    }
}


// processes the heartbeat of the CS
class initiate_heartbeat extends Thread
{
    private PrintWriter sendhb;
    String myID;
    boolean alive = true;
    // Constructor which initiates printwriter and CS ID
    public initiate_heartbeat(Socket hb_css, String myid)
    {
        try {
            sendhb = new PrintWriter(hb_css.getOutputStream());
            myID = myid;
        } catch (Exception e) {
            System.out.println("48- Content server error on hearbeat connection... " + e);
        }
    }

    public void run()
    {
       try {
        // sends a beep along with id of CS every 11 seconds
            while(alive)
            {
                sendhb.println(myID + " Beep!");
                sendhb.flush();
                Thread.sleep(11000);
            }
       } catch (Exception e) {
            System.out.println("183: CS HB thread error: "+e);
       }
    }
}


class LamportClockCS
{
    public int myLC;
    
    public LamportClockCS()
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


