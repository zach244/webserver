/**
 * This is the separate thread that services each
 * incoming echo client request.
 *
 * @author Greg Gagne
 */

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

public class Connection implements Runnable {
    public static final int BUFFER_SIZE = 256;
    private Socket client;
    private Configuration configuration;
    byte[] buffer = new byte[BUFFER_SIZE];
    URL url = null;
    BufferedReader in = null;
    OutputStream out = null;

    public Connection(Socket client, Configuration configuration) {
        this.client = client;
        this.configuration = configuration;
    }

    /**
     * This method runs in a separate thread.
     */
    public void run() {
        try {
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream out =
                    new BufferedOutputStream(client.getOutputStream());

            String line;
            String[] splits;
            String status = null;
            String date = null;
            String serverName = null;
            String contentType = null;
            String contentLength = null;
            String logLine = null;
            int logStatus;
            String logDate = null;


            line = in.readLine(); //reads in first line then which is the request
            splits = line.split(" "); //splits request based on spaces
            if (splits[1].equals("/") || splits[1].equals(" "))//if the request just has a / serves default document
            {
                splits[1] = configuration.getDefaultDocument(); //get default document
                splits[1] = splits[1].substring(splits[1].lastIndexOf("/"));//remove / infront of index.html
            }
            File f = new File(configuration.getDocumentRoot() + splits[1].substring(1)); //create a new file with the request

            if (f.exists() && !f.isDirectory()) { //if file exists create 200 Ok header
                status = "HTTP/1.1 200 OK" + "\r\n";//set status to 200 ok
                contentLength = "Content-Length: " + f.length() + "\r\n"; //Content-length Header
                logStatus = 200;

            } else { //if file doesn't exist create 404 header and give the path to 404.html
                status = "HTTP/1.1 404 Not Found" + "\r\n"; //write 404 status
                splits[1] = configuration.getFourOhFourDocument(); //get 404 location
                splits[1] = splits[1].substring(splits[1].lastIndexOf("/")); //remove / before 404.html
                contentLength = "Content-Length: " + configuration.getFourOhFourDocument() + "\r\n"; //Content-length Header
                logStatus = 404;
            }

            String fileType = splits[1].substring(splits[1].lastIndexOf(".")); //get the filetype of the request

            if (fileType.equals(".html")) //based on filetype of request, create proper content-type
                contentType = "Content-Type: " + "text/html" + "\r\n";
            else if (fileType.equals(".gif"))
                contentType = "Content-Type: " + "image/gif" + "\r\n";
            else if (fileType.equals(".jpg"))
                contentType = "Content-Type: " + "image/jpeg" + "\r\n";
            else if (fileType.equals(".png"))
                contentType = "Content-Type: " + "image/png" + "\r\n";
            else
                contentType = "Content-Type: " + "text/plain" + "\r\n";


            Calendar calendar = Calendar.getInstance(); //get calandar instance
            SimpleDateFormat dateFormat = new SimpleDateFormat( //format calandar
                    "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT")); //set GMT timezone
            date = dateFormat.format(calendar.getTime()); //get the date
            logDate = dateFormat.format(calendar.getTime());
            date = "Date: " + date + "\r\n"; //create the date header
            serverName = "Server: " + configuration.getServerName() + "\r\n"; //get the serverName header
            String fullHeader = status + date + contentType + serverName //Construct the full Header including Connection: close
                    + contentLength + "Connection: close\r\n\r\n";
            splits[1] = splits[1].substring(1); //need to remove leading / from file path
            out.write(fullHeader.getBytes()); //write the header to the client
            out.flush();

            InputStream file = new BufferedInputStream(new FileInputStream(splits[1])); //create an inputstream to get the file
            byte[] buffer = new byte[BUFFER_SIZE];
            int numBytes;
            while ((numBytes = file.read(buffer)) != -1) { //write the file to the client
                out.write(buffer, 0, numBytes);
            }
            out.flush(); //flush

            File logFile = new File(configuration.getLogFile()); //create the Logfile Object
            File logParentDirectory = new File(logFile.getParent());//used to see if Log directory is valid
            if (!logFile.exists() && !logParentDirectory.isDirectory()) { //if the log file doens't exist and parent directory doesn't exist
                logParentDirectory.mkdir(); //create new directory
                logFile.createNewFile(); //create new logfile
            } else if (!logFile.exists() && logParentDirectory.isDirectory()) { // if the log file doesn't exist and the directory does
                logFile.createNewFile(); //create new log file
            }

            logLine = "\n" + client.getLocalAddress().toString() + " " + "[" + logDate + "]" + " " //write to LogFile
                    + line + " " + logStatus + " " + contentLength;
            PrintWriter logWriter
                    = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true))); //create FileWriter with true to allow appending
            logWriter.println(logLine);//append logline onto logfile

            file.close(); //close all connections
            in.close();
            out.close();
            logWriter.close();
            client.close();

        } catch (java.io.IOException ioe) {
            System.err.println(ioe);
        }

    }
}
