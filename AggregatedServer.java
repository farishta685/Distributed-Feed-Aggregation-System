import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AggregatedServer
{
    private static File agFeed;
    private static LamportClockAS LampClock;
    private static File LampClockRecord;

    public AggregatedServer()
    {
    }

    // test script will check if you want the server to start for the first time using start
    // or if you want the server to recover using rec
    public void Testing(String status, int port) throws NumberFormatException, IOException
    {
        if(status.startsWith("rec"))
        {
            // since in recovery mode reassign feeds to the existing aggregation feed and lamportclock records file
            agFeed = new File("aggregationFeed.txt");
            LampClockRecord = new File("LampClockRecord.txt");
            if (LampClockRecord.createNewFile())
                {
                    System.out.println("50-created Lamport clock record file for the first time!");
                } else {
                    System.out.println("52-LampClock Record file already exists!");
                }

            // initialise lamport clock and update with the last entry in the LampcloclRecord file
            LampClock = new LamportClockAS();
            FileReader readfile = new FileReader(LampClockRecord);
            BufferedReader readLC = new BufferedReader(readfile);
            String new_lc = readLC.readLine();
            while(new_lc != null)
            {
                new_lc = readLC.readLine();
                if(new_lc != null)
                {
                    LampClock.updateLC(Integer.parseInt(new_lc));
                    break;
                }
            }
            readLC.close();

            // now listen for connections and execute threads for new requests
            try {
                ServerSocket ss = new ServerSocket(port);
                for (int i = 0; i<200; i++)
                {
                    Socket requests = ss.accept();
                    System.out.println("New connection accepted! adding client "+i);
                    Thread mynewthread = new Request_processor(requests, agFeed, LampClock, LampClockRecord);
                    mynewthread.start();
                }
                ss.close();
             } catch (IOException e) {
                // System.out.println("58- AS main error..."+e);
            }
        }

        // else if statying with start then it means we are starting server for the first time 
        else if(status.startsWith("st"))
        {
            try {
                // initialise lampclock and create new lampclockrecord file
                LampClock = new LamportClockAS();
                
                agFeed = new File("aggregationFeed.txt"); 

                // write first entry into LCrecord
                LampClock.writeLC();
                // initialise sockets and listen for connection and start new threads
                ServerSocket ss = new ServerSocket(port);
                for (int i = 0; i<200; i++)
                {
                    Socket requests = ss.accept();
                    Thread mynewthread = new Request_processor(requests, agFeed, LampClock, LampClockRecord);
                    mynewthread.start();
                }
                ss.close();

             } catch (IOException e) {
                // System.out.println("58- AS main error..."+e);
            }
        }

    }

    public static void main(String[] args) throws NumberFormatException, IOException
    {
        // parse arguements and run Testing
        String status = args[0];
        int port = Integer.parseInt(args[1]);

        AggregatedServer AS = new AggregatedServer();
        AS.Testing(status, port);

    }
}


// this thread process each request recieved by the CS and GC 
class Request_processor extends Thread
{
    private BufferedReader readRequest; 
    private PrintWriter writeResponse;
    private File AgFeed;
    private Socket requestSocket;
    private static LamportClockAS myLampC;

    // constructor initialises the socket and the buffers to read and write response
    public Request_processor(Socket mySocket, File agFeed, LamportClockAS lc, File Lampclockrecord)
    {
        try {
            requestSocket = mySocket;
            readRequest = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));
            writeResponse = new PrintWriter(requestSocket.getOutputStream());
            AgFeed = agFeed;
            myLampC = lc;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // removes the feeds from the CS with the given ID
    public static void removeFeed(File AgFeed, String csID)
    {
        
        try {
            File tempFeed = new File("tempfile.txt");
            BufferedWriter write = new BufferedWriter(new FileWriter(tempFeed, true));
            BufferedReader read = new BufferedReader(new FileReader(AgFeed));
            PrintWriter writeToTempFeed = new PrintWriter(write);
            String nextline = read.readLine();
            while(nextline != null)
            {
                if(nextline.equals(csID))
                {
                    nextline = read.readLine();
                    while(!(nextline.equals("</feed>")))
                    {
                        nextline = read.readLine();
                    }
                    nextline = read.readLine();
                    continue;
                }
                writeToTempFeed.println(nextline);
                nextline = read.readLine();
            }

            // rename tempfile to aggregationfeed and delete old aggregation feed and 
            writeToTempFeed.println();
            Path source = Paths.get("tempfile.txt");
            Path ogpath = Paths.get("aggregationFeed.txt");
            Files.delete(ogpath);
            Files.move(source, source.resolveSibling("aggregationFeed.txt"));
            
            read.close();
            writeToTempFeed.close();
            write.close();

        } catch (Exception e) {

        }
    }
    
    // listens for the heartbeat of the CS
    public void listen_hearbeat(File Agfeed) 
    {
        try {        
            String heartbeat = readRequest.readLine();
            String temp_heartbeat = heartbeat;
            String csID = "myID: ";
            temp_heartbeat = temp_heartbeat.substring(0,heartbeat.length()-6);
            csID += temp_heartbeat;
            while(true)
            {
                if(heartbeat == null)
                {
                    readRequest.close();   
                    removeFeed(Agfeed, csID);
                    break;
                }
                else if (heartbeat != null)
                {
                    Thread.sleep(12000);
                    heartbeat = readRequest.readLine();
                }
                
            }
        } catch (Exception e) {
        // System.out.println("229- Agregation server: hearbeat failure: " + e);
        }
        
    }

    // handles the put request from the CS
    public void handlePUTrequests()
    {
        try {
            BufferedWriter bfr = new BufferedWriter(new FileWriter(AgFeed, true));
            PrintWriter writeToFeed = new PrintWriter(bfr);
            String contentRequest = readRequest.readLine();
            writeToFeed.append(contentRequest);
            

            while(readRequest.ready())
            {
                // using readIn and filewriter read each line of user request and put it into the aggregationFeed.txt files
                contentRequest = readRequest.readLine();
                writeToFeed.append(contentRequest);
                writeToFeed.append("\n");

                // if we have reached end of feed break loop 
                if (contentRequest.startsWith("</feed>"))
                {
                    writeToFeed.close();
                    break;
                }
            }

        } catch (Exception e){
        //    System.out.println("99- contentHandler error for put request... " + e);
        }
    }

    // send the entire aggregation feed to the getclient
    public void handleGETrequests()
    {
        try {
            File getFeed = new File("aggregationFeed.txt");
            FileReader readfile = new FileReader(getFeed);
            BufferedReader myReader = new BufferedReader(readfile);
            String line = myReader.readLine();
            
            while(line != null)
            {
                writeResponse.println(line);
                line = myReader.readLine();
            }
            writeResponse.flush();
            myReader.close();

        } catch (Exception e) {
            // System.out.println("111- Aggregation server: clienthandler error..."+e);
        }
    }


    public void run()
    {
        try {
            Thread.sleep(1000);
            while(true)
            {
                // read the request header
                // if request is a PUT or GET then read its lamport clock and update mine then call the right method to handle
                // then increment my lamport clock and send response carrying my lamport clock to client or CS
                String newRequet = readRequest.readLine();
                if(newRequet.startsWith("PUT"))
                {
                    int clientLC = Integer.parseInt(readRequest.readLine());
                    myLampC.updateLC(clientLC);
                    handlePUTrequests();
                    writeResponse.println("HTTP/201 : feed aggregation successful!");
                    myLampC.incrementLC();
                    writeResponse.println(myLampC.getLC());
                    writeResponse.flush();
                    // myLampC.writeLC();
                
                } else if (newRequet.startsWith("GET"))
                {
                    int clientLC = Integer.parseInt(readRequest.readLine());
                    myLampC.updateLC(clientLC);
                    writeResponse.println("HTTP/201 : sending feed now!");
                    myLampC.incrementLC();
                    writeResponse.println(myLampC.getLC());
                    writeResponse.flush();
                    // myLampC.writeLC();
                    handleGETrequests();

                }
                // if its a heartbeat thread then direct it to the HB function 
                else if (newRequet.endsWith("Beep!"))
                {
                    listen_hearbeat(AgFeed);

                }
                // else if its a ping then take the CS request id and check if its been processed if so send HTTP/200
                // if not then ask them to retry 
                else if (newRequet.startsWith("Ping!"))
                {
                    String ID = readRequest.readLine();
                    BufferedReader read = new BufferedReader(new FileReader(AgFeed));
                    String line = read.readLine();
                    Boolean processed = false;
                    while(line != null)
                    {
                        if (line.equals(ID))
                        {
                            processed = true;
                            break;
                        }
                        else
                        {
                            line = read.readLine();
                        }
                    }
                    if (processed == true)
                    {
                        writeResponse.println("HTTP/200 PUT Request processed successfully!");
                    }
                    else
                    {
                        writeResponse.println("Retry Request please!");
                    }
                    writeResponse.flush();
                    read.close();
                }
                else{
                    writeResponse.println("400 error! wrong HTTP method requested!");
                    writeResponse.flush();
                }
            }
            
        } catch (Exception e) {
            // System.out.println("162- some request processing error: "+e);
        }
    }
}


class LamportClockAS 
{
    private File myLCrecord;
    public static int myLC;
    // public static BufferedWriter bfr;
    
    public LamportClockAS() throws IOException
    {
        myLCrecord = new File("LampClockRecord.txt");
        // bfr = new BufferedWriter(new FileWriter(myLCrecord, true));
        myLC = 0;
    }

    public int getLC()
    {
        return myLC;
    }

    public void writeLC() throws IOException
    {
        BufferedWriter bfr = new BufferedWriter(new FileWriter(myLCrecord, true));
        PrintWriter writeToFeed = new PrintWriter(bfr);
        writeToFeed.append(Integer.toString(myLC)+"\n");
        writeToFeed.close();
    }

    public void incrementLC() throws IOException
    {
        myLC += 1;
        // writeLC();

    }
    public int updateLC(int requestLC) throws IOException
    {
        if( myLC >= requestLC)
        {
            myLC = myLC +1; 
            // writeLC();
            return myLC;
        }
        else {
            myLC = requestLC+1;
            // writeLC();
            return myLC;
        }
        
    }
}

