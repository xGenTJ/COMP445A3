package main;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;

@Command(name = "get", description = "Get parser")
final public class get implements Runnable{
    @Option(names = {"-v", "-verbose"}, description = "Display the verbose information")
    boolean verbose;

    @Option(names = {"-h"}, description = "Display the headers")
    LinkedHashMap<String, String> headers;

    @Option(names = {"http://"}, description = "url")
    URL url;

    @Option(names = {"-p"}, description = "port")
    int port;

    @Option(names = {"-d"}, description = "path to directory")
    String pathToDir;

    boolean fileServer;
    @Override
    public void run(){
        //parse url

        if (port == 0)
        {
            port = 80; // default port
        }
        String path, query, host;
        if(url != null) {
            path = url.getPath();
            query = url.getQuery();
            host = url.getHost();
            fileServer = false;
        }
        else {
            path = pathToDir;
            fileServer = true;
            // need to check how to handle this properly
            query = null;
            host = null;
        }

//FOR TESTING PURPOSES
        System.out.println("\nhttpc get running...");
        System.out.println(verbose );
        System.out.println(url);
        System.out.println("path: " + path +
                "\nquery: " + query +
                "\nhost: " + host);
        System.out.println("fileServer: " + fileServer);
//        if ((headers != null) && (!headers.isEmpty()))
//            headers.forEach((key,value)->//System.out.println(key + ": " + value));

        //System.out.println("-----------------------------");

        System.out.println("-----------------------------");

        if(verbose){
            //expected to print all the status and its headers, then contents of the response
            try {
//                String path, LinkedHashMap<String,String> headers, String address, String  query, boolean fileServer
                UDPClient.createGET(path, headers, host, query, fileServer);
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
                List<byte[]> bytelist = UDPClient.createGET(path, headers, host, query, fileServer);
                //    initiliaze ports
                SocketAddress routerAddress = new InetSocketAddress("127.0.0.1", 3000);
                InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 8007);
                InetSocketAddress clientAddress = new InetSocketAddress("127.0.0.1", 41830);


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
