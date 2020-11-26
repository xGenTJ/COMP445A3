package main;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

public class UDPServer {

    private static final Logger logger = LoggerFactory.getLogger(UDPServer.class);
    private static BufferedWriter wr;
    private String inputPayload = "";
    private String request = "";
    private int portN;
    private LinkedHashMap<String, String> headersMap = new LinkedHashMap<>();

    public UDPServer (String hostName, int portNumber){
        System.out.println("UDP Server Constructor");
        InetSocketAddress serverAddress = new InetSocketAddress(hostName, portNumber);
        SocketAddress routerAddress = new InetSocketAddress(hostName, 3000);
    }

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(asList("port", "p"), "Listening port")
                .withOptionalArg()
                .defaultsTo("8007");

        OptionSet opts = parser.parse(args);
        int port = Integer.parseInt((String) opts.valueOf("port"));
        UDPServer server = new UDPServer("127.0.0.1", 8007);
        server.listenAndServe(port);
    }

    private void listenAndServe(int port) throws IOException {

        try (DatagramChannel channel = DatagramChannel.open()) {


            channel.bind(new InetSocketAddress(port));
            logger.info("EchoServer is listening at {}", channel.getLocalAddress());
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);
            boolean sentSYNACK = false;
            int numberOfDataPacketsReceived = 0;
            int numberOfExpectedDataPackets = 0;


            for (; ; ) {
                buf.clear();
                SocketAddress router = channel.receive(buf);

                // Parse a packet from the received raw data.
                buf.flip();
                Packet recvPacket = Packet.fromBuffer(buf);
                buf.flip();

                String payload = new String(recvPacket.getPayload(), UTF_8);
                logger.info("Packet: {}", recvPacket);
                logger.info("Payload: {}", payload);
                logger.info("Router: {}", router);


                // Send the response to the router not the client.
                // The peer address of the packet is the address of the client already.
                // We can use toBuilder to copy properties of the current packet.
                // This demonstrate how to create a new packet from an existing packet.
                if (recvPacket.getType() == 1) // if it's an SYN
                {
                    if (!sentSYNACK)
                    {
                        Packet SYNACK = recvPacket.toBuilder()
                                .setType(2)
                                .setSequenceNumber(0)
                                .setPayload(new byte[0])
                                .create();
                        channel.send(SYNACK.toBuffer(), router);
                        sentSYNACK = true;
                    }
                }
                else if (recvPacket.getType() == 3) //if it's an ACK
                {
                    numberOfExpectedDataPackets =  Integer. parseInt(payload);
                }
                else if (recvPacket.getType() == 0) //if it's an ACK
                {
                    inputPayload += payload;
                    numberOfDataPacketsReceived++;

                }

                if (numberOfDataPacketsReceived == numberOfExpectedDataPackets) //if all the data packets have been received, then process the request
                {
//                    getRequest(inputPayload);
//                    Packet response = recvPacket.toBuilder()
//                            .setType(0)
//                            .setSequenceNumber(0)
//                            .setPayload(sendResponse(StatusCode.OK, "1.0", true, "", "", "").getBytes())
//                            .create();
//                    channel.send(SYNACK.toBuffer(), router);
                }

            }
        }
    }



    public void getRequest(String request) throws IOException {
        try {
            StatusCode statusCode = StatusCode.OK;
            boolean headerExists = false, payloadExists = false;
            String headers = "", payload = "", returnBody = "", fileDir = "";

            //split request into firstLine and headersAndBody
            String firstLine = request.split("\r\n", 2)[0];
            //split the first line into seperate stuff
            String[] firstLineStringArray = firstLine.split(" ");
            String httpVersion = firstLineStringArray[firstLineStringArray.length - 1];
            String requestURL = firstLineStringArray[firstLineStringArray.length - 2];
            String requestType = firstLineStringArray[0];

            if (requestType.equalsIgnoreCase("GET")) {

                //traditional GET request
                if (requestURL.contains("http://")) {
                    System.out.println("TRADITIONAL REQUEST");
                    //check if there's a query string
                    if (requestURL.contains("?")) {
                        //split and parse the query string
                        try {

                            String queryString = requestURL.substring(requestURL.indexOf("?") + 1);
                            System.out.println("SERVER QUERY STRING: " + queryString);
                            returnBody += "\r\n=== QUERIES ===\r\n";
                            for (String pair : queryString.split("&")) {

                                String key = pair.substring(0, pair.indexOf("="));
                                String value = pair.substring(pair.indexOf("=") + 1);
                                System.out.println("SERVER QUERY PAIR: " + key + " | " + value);

                                returnBody += key + ": " + value + "\r\n";
                            }
                        } catch (Exception e) {
                            System.out.println("QUERY STRING EMPTY");
                        }
                    }

                    //header parsing
                    if (request.substring(request.indexOf("\r\n") + 1).trim().equals("")) {
                        System.out.println("ERROR: NO HEADERS");
                    } else {
                        headers = request.split("\r\n", 2)[1];
                        String[] headerPairs = headers.split("\r\n");
                        returnBody += "\r\n=== HEADERS ===\r\n";
                        for (String headerPair : headerPairs) {

                            if (!headerPair.trim().equalsIgnoreCase("")) {

                                returnBody += headerPair + "\r\n";
                                String headerName = headerPair.split(":", 2)[0];
                                String headerValue = headerPair.split(":", 2)[1];
                                headersMap.put(headerName, headerValue);
                            }
                        }
                    }
                }
                //fileServer shizzz
                else {
                    System.out.println("FILESERVER REQUEST");

                    System.out.println("SERVER REQUEST URL: " + requestURL);
                    requestURL = requestURL.replace("/", "\\");
                    String absolutePath = System.getProperty("user.dir") + "\\src\\main\\java\\FileServer" + requestURL;
                    System.out.println("ABSOLUTE PATH:" + absolutePath);

                    if (!absolutePath.contains(System.getProperty("user.dir") + "\\src\\main\\java\\FileServer") || absolutePath.contains("\\..")) {
                        statusCode = StatusCode.FORBIDDEN;
                        sendResponse(statusCode, httpVersion, headerExists, headers, fileDir, returnBody);
                        System.exit(0);
                    }

                    File f = new File(absolutePath);
                    // if path exists
                    if (f.exists()) {
                        System.out.println("PATH EXISTS...");

                        //if path is a directory
                        if (!f.isFile()) {

                            System.out.println("PATH IS A DIRECTORY...");


                            //if directory is not empty
                            if (f.list().length != 0) {

                                System.out.println("DIRECTORY NOT EMPTY...");
                                returnBody += "\r\n=============== LIST OF FILES ===============\r\n";
                                for (String pathname : f.list()) {
                                    // Print the names of files and directories
                                    returnBody += "\r\n" + pathname +"\r\n";
                                    System.out.println(pathname);
                                }
                            }
                            //if directory is empty
                            else {
                                System.out.println("DIRECTORY IS EMPTY...");
                            }
                        }
                        //if path is a file
                        else {
                            System.out.println("PATH IS A FILE...");

                            //if file is not empty
                            if (f.length() != 0) {

                                System.out.println("READING FILE...");
                                returnBody += "\r\n=============== FILE BODY ===============\r\n";
                                //assuming it's a GET
                                Scanner myReader = new Scanner(f);
                                while (myReader.hasNextLine()) {
                                    String data = myReader.nextLine();
                                    returnBody += "\r\n" + data + "\r\n";;
                                    System.out.println(data);
                                }
                            }
                            //if file is empty
                            else {
                                System.out.println("FILE IS EMPTY...");
                            }
                        }

                    }
                    //if file doesn't exist
                    else {
                        System.out.println("FILE/DIRECTORY DOESNT EXIST...");
                        statusCode = StatusCode.NOT_FOUND;
                    }
                }
            } else if (requestType.equalsIgnoreCase("POST")) {

//traditional POST request
                if (requestURL.contains("http://")) {

                    if (requestURL.contains("?")) {
                        //split and parse the query string
                        try {

                            String queryString = requestURL.substring(requestURL.indexOf("?") + 1);
                            System.out.println("SERVER QUERY STRING: " + queryString);
                            returnBody += "\r\n=== QUERIES ===\r\n";
                            for (String pair : queryString.split("&")) {

                                String key = pair.substring(0, pair.indexOf("="));
                                String value = pair.substring(pair.indexOf("=") + 1);
                                System.out.println("SERVER QUERY PAIR: " + key + " | " + value);

                                returnBody += key + ": " + value + "\r\n";
                            }
                        } catch (Exception e) {
                            System.out.println("QUERY STRING EMPTY");
                        }
                    }

                    //if there's no body
                    if (request.substring(request.indexOf("\r\n\r\n") + 1).trim().equals("")) {
                        //if there's no headers
                        if (request.substring(request.indexOf("\r\n") + 1).trim().equals("")) {
                            System.out.println("NO HEADERS");
                        }
                        //if there's headers and no body
                        else {
                            headers = request.split("\r\n", 2)[1];
                            headerExists = true;
                        }
                    }
                    //if there's a body
                    else {
                        //if there's no header
                        if (request.substring(request.indexOf("\r\n") + 1, request.indexOf("\r\n") + 2).trim().equals("")) {
                            payload = request.split("\r\n\r\n", 2)[1];

                            payloadExists = true;
                        }
                        //if there's a header and a body
                        else {
                            String headersAndBody = request.split("\r\n", 2)[1];

                            headers = headersAndBody.split("\r\n\r\n")[0];

                            if (headersAndBody.split("\r\n\r\n").length > 1) {
                                payload = headersAndBody.split("\r\n\r\n")[1];
                            }

                            headerExists = true;
                            payloadExists = true;
                        }

                    }
//                    System.out.println(headerExists);
//                    System.out.println(payloadExists);

                    //header parsing
                    if (headerExists) {
                        String[] headerPairs = headers.split("\r\n");
                        returnBody += "\r\n=== HEADERS ===\r\n";
                        for (String headerPair : headerPairs) {

                            if (!headerPair.trim().equalsIgnoreCase("")) {
                                returnBody += headerPair + "\r\n";
                                String headerName = headerPair.split(":", 2)[0];
                                String headerValue = headerPair.split(":", 2)[1];
                                headersMap.put(headerName, headerValue);

                            }

                        }
                    }
                    if (payloadExists) {
                        //payload parsing
                        System.out.println("\n===Payload ===\n" + payload);
                        returnBody += "\n===Payload ===\n" + payload + "\n";
                        //Json parsing
                        JSONObject json = new JSONObject(payload);

                        Iterator<String> keys = json.keys();

                        while (keys.hasNext()) {
                            String key = keys.next();
                            if (json.get(key) instanceof JSONObject) {

                                System.out.println("\n===JSON Object ===\n" + json.get(key));
                                returnBody += "\n===JSON Object ===\n" + json.get(key) + "\n";
                            } else {
                                System.out.println("\n===JSON Object ===\n" + "Key: " + key + " | " + "Value: " + json.get(key));
                                returnBody += "\n===JSON Object ===\n" + "Key: " + key + " | " + "Value: " + json.get(key) + "\n";
                            }
                        }
                    }
                }
                //HTTPFS
                else {

                    System.out.println("SERVER REQUEST URL: " + requestURL);
                    requestURL = requestURL.replace("/", "\\");
                    String absolutePath = System.getProperty("user.dir") + "\\src\\main\\java\\FileServer" + requestURL;
                    System.out.println("ABSOLUTE PATH:" + absolutePath);

                    if (!absolutePath.contains(System.getProperty("user.dir") + "\\src\\main\\java\\FileServer")) {
                        statusCode = StatusCode.FORBIDDEN;
                        sendResponse(statusCode, httpVersion, headerExists, headers, fileDir, returnBody);
                        System.exit(0);
                    }

                    String fileBody = request.split("\r\n\r\n")[1];

                    //added this remove - and '{ }' from the body content
                    fileBody = fileBody.replace("-", " ");
                    fileBody = fileBody.substring(fileBody.indexOf("'{") + 2, fileBody.indexOf("}'"));
                    System.out.println(fileBody);

                    File f = new File(absolutePath);
                    //create the parent directories if they don't exist
                    System.out.println("POSSIBLY MAKING THE PARENT DIRECTORIES...");
                    if (f.getParentFile() != null) {
                        f.getParentFile().mkdirs();
                    }

                    //if file doesn't exist and isn't a file
                    if (!f.exists() && !f.isFile()) {
                        f.createNewFile();
                        Writer fileWriter = new FileWriter(f, false); //overwrites file
                        fileWriter.write(fileBody);
                        returnBody += "\r\n" + fileBody;
                        fileWriter.close();
                        System.out.println("NEW FILE CREATED...");
                        statusCode = StatusCode.CREATED;
                    }
                    //if file exists, overwriiiiiiiiiiiiiiiiiiiiiiiiiiiiite
                    else {
                        Writer fileWriter = new FileWriter(f, false); //overwrites file
                        fileWriter.write(fileBody);
                        returnBody += "\r\n" + fileBody;
                        fileWriter.close();
                        System.out.println("OVERWRITNG EXISTING FILE...");
                        statusCode = StatusCode.OK;
                    }

                }


            }                //error statuscode will be intantiated in exception handling cases
            sendResponse(statusCode, httpVersion, headerExists, headers, fileDir, returnBody);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(StatusCode.BAD_REQUEST, "1.0", false, "", "", "");
        }
    }

    public List<byte[]> sendResponse(StatusCode statusCode, String httpVersion, Boolean headerExists, String headers, String fileDir, String returnBody) throws IOException {
        String response = "";

        LocalDateTime date = LocalDateTime.now();

        response += "HTTP/" + httpVersion + " " + statusCode.code + " " + statusCode.phrase + "\r\n" +
                "Server: http://localhost:" + portN + "\r\n" +
                "Date: " + date + "\r\n";

        if (headerExists) {
            response += headers;
        }

        if (fileDir != "") {
            response += "\n" + fileDir;
        }

        System.out.println(response);

        byte[] packetByteOutput = new byte[1013];
        List<byte[]> bytelist = new ArrayList<>();

        //sending data
        byte[] byteArray = response.getBytes();
        int i = 0;

        for (byte b : byteArray) {
            if (i < 1013) {
                packetByteOutput[i] = b;
                i++;
            } else    //when i reaches 4
            {
                bytelist.add(packetByteOutput);

                //reset i and packetByte
                i = 0;
                packetByteOutput = new byte[1013];
                packetByteOutput[i] = b;
            }
        }

        // if there is a leftover of size < 1013: put in list
        if (byteArray.length % 1013 != 0) {
            bytelist.add(packetByteOutput);
        }

        return bytelist;

    }

    private static Packet recvPacket(DatagramChannel channel) throws IOException {

        // Try to receive a packet within timeout.
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_READ);
        logger.info("Waiting for the response");
        selector.select(5000);

        Set<SelectionKey> keys = selector.selectedKeys();
        if(keys.isEmpty()){
            logger.error("No response after timeout");
//            return "";
        }
        // We just want a single response.
        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN).order(ByteOrder.BIG_ENDIAN);
        SocketAddress router = channel.receive(buf);
        buf.flip();
        Packet resp = Packet.fromBuffer(buf);
//            logger.info("Packet: {}", resp);
//            logger.info("Router: {}", router);
//            logger.info("Payload: {}",  payload);
        String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
        keys.clear();
        return resp;
    }
}