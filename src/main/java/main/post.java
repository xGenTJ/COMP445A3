package main;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;


@Command(name = "post", description = "Get parser")
final public class post implements Runnable{
    @Option(names = {"-v", "-verbose"}, description = "Display the verbose information")
    boolean verbose;

    @Option(names = {"-h"}, description = "Display the headers")
    LinkedHashMap<String, String> headers;

    @Option(names = {"-d"}, description = "inline-data")
    String inlineData;

    @Option(names = {"-f"}, description = "file data")
    String file;

    @Option(names = {"http://"}, description = "url")
    URL url;

    @Option(names = {"-p"}, description = "port")
    int port;

    @Option(names = {"-d2"}, description = "path to directory")
    String pathToDir;

    boolean fileServer;
    @Override
    public void run(){

        if (port == 0)
        {
            port = 80; // default port
        }

        //POST(boolean verbose, String path, LinkedHashMap<String,String> headers, LinkedHashMap<String,String> parameters, String inline, String address)
        //parse url
        String path, query, host;
        if(url != null) {
            path = url.getPath();
            query = url.getQuery();
            host = url.getHost();
            fileServer = false;
        }
        else {
            path = pathToDir;
            // need to check how to handle this properly
            query = null;
            host = null;
            fileServer = true;
        }
        String body = null;

        //System.out.println("\nhttpc post running...");
        //System.out.println(verbose);
        //System.out.println(url);
        //System.out.println("path: " + path +
//                "\nquery: " + query +
//                "\nhost: " + host);
//        if ((headers != null) && (!headers.isEmpty()))
//            headers.forEach((key,value)->//System.out.println(key + ": " + value));

        if(inlineData != null){
            if(!(inlineData.isEmpty()))
            {
                body = inlineData;
                System.out.println("\ninline body:" + body);
            }
        }
        if(file != null) {
            if (!(file.isEmpty())) {
                body = file;
                System.out.println("\nfile body:" + body + "\nfile bodylength:" + body.length());
            }
        }
        if((inlineData != null) && (file != null))
        {
            body = inlineData;
            //System.out.println("\nbody:" + body);
        }

        System.out.println("----------------------------- END OF HTTPC PRINTING -----------------------------");

        //read inline data or data from file and assign it to a String
        //handle empty file
        //pass string to post request

        if(verbose){
            //expected to print all the status and its headers, then contents of the response
            try {
//                String path, LinkedHashMap<String,String> headers, String body, String address, String query, boolean fileServer
                UDPClient.createPOST(path, headers, body, host, query, fileServer);
            } catch (Exception e) {
//                e.printStackTrace();
                StatusCode statusCode = StatusCode.BAD_REQUEST;
                System.out.println("HTTP/1.0 " + statusCode.code + " " + statusCode.phrase);
                System.exit(0);
            }
        }
        else //not verbose option
        {
            try {
                List<byte[]> bytelist = UDPClient.createPOST(path, headers, body, host, query, fileServer);
                //    initiliaze ports
                SocketAddress routerAddress = new InetSocketAddress("192.168.2.10", 3000);
                InetSocketAddress serverAddress = new InetSocketAddress("192.168.2.3", 8007);
                InetSocketAddress clientAddress = new InetSocketAddress("192.168.2.125", 41830);

                //    3 way dick shake
                long sequenceNumber = 0;

                try {
                    sequenceNumber = UDPClient.threeWayHandShake(routerAddress, serverAddress, bytelist.size());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                UDPClient.sendDataPacket(routerAddress, serverAddress, bytelist);





            } catch (Exception e) {
//                e.printStackTrace();
                StatusCode statusCode = StatusCode.BAD_REQUEST;
                System.out.println("HTTP/1.0 " + statusCode.code + " " + statusCode.phrase);
                System.exit(0);
            }
        }
    }
}
