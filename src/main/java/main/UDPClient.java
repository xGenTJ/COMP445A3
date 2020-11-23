package main;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_READ;

public class UDPClient {

    int packetType = 0; //0 -> data, 1 -> SYN, 2 -> SYN-ACK, 3 - ACK 4- NAK

    private static final Logger logger = LoggerFactory.getLogger(UDPClient.class);


    private static void runClient(SocketAddress routerAddr, InetSocketAddress serverAddr) throws IOException {


        try(DatagramChannel channel = DatagramChannel.open()){

            Object[] objectArray = createOutputPayload();

            String outputPayload = (String) objectArray[0];  //from get or post
            int packetType = (int) objectArray[1]; //0 -> data, 1 -> SYN, 2 -> SYN-ACK, 3 - ACK 4- NAK
            int sequenceNumber = 0;
            byte[] packetByteOutput = new byte[1013];
            List<byte[]> bytelist = new ArrayList<>();


            //SENDING SYN
            if (packetType == 1)
            {
                Packet p = new Packet.Builder()
                        .setType(packetType)
                        .setSequenceNumber(sequenceNumber)
                        .setPortNumber(serverAddr.getPort())
                        .setPeerAddress(serverAddr.getAddress())
                        .create();
                channel.send(p.toBuffer(), routerAddr);
                sequenceNumber++;
            }
            //sending data
            else if (packetType == 0) {
                byte[] byteArray = outputPayload.getBytes();
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

                // if there is a leftover of size < 4: put in list
                if (byteArray.length % 1013 != 0) {
                    bytelist.add(packetByteOutput);
                }


                //send packets
                for (byte[] b : bytelist) {
                    Packet p = new Packet.Builder()
                            .setType(packetType)
                            .setSequenceNumber(sequenceNumber)
                            .setPortNumber(serverAddr.getPort())
                            .setPeerAddress(serverAddr.getAddress())
                            .setPayload(b)
                            .create();
                    channel.send(p.toBuffer(), routerAddr);
                    sequenceNumber++;
                }
            }

            logger.info("Sending \"{}\" to router at {}", outputPayload, routerAddr);

            // Try to receive a packet within timeout.
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            logger.info("Waiting for the response");
            selector.select(5000);

            Set<SelectionKey> keys = selector.selectedKeys();
            if(keys.isEmpty()){
                logger.error("No response after timeout");
                return;
            }

            // We just want a single response.
            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
            SocketAddress router = channel.receive(buf);
            buf.flip();
            Packet resp = Packet.fromBuffer(buf);
            logger.info("Packet: {}", resp);
            logger.info("Router: {}", router);
            String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
            logger.info("Payload: {}",  payload);

            keys.clear();
        }
    }
    public static Object[] SYN()
    {
        return new Object[] {"",1};
    }

    public static Object[] ACK()
    {
        return new Object[] {"",2};
    }

    public static Object[] SYNACK()
    {
        return new Object[] {"",3};
    }

    public static Object[] NAK()
    {
        return new Object[] {"",4};
    }
    public static Object[] GET(String path, LinkedHashMap<String,String> headers, String address, String  query, boolean fileServer) throws Exception {

        String outputString = "";
//
        if (!fileServer) {

            if (query != null) {
                if (path != null) {
                    outputString = "GET " + "http://" + address + path + "?" + query + " HTTP/1.0\r\n";
                } else {
                    outputString = "GET " + "http://" + address + "?" + query + " HTTP/1.0\r\n";
                }
            } else {
                if (path != null) {
                    outputString = "GET " + "http://" + address + path + " HTTP/1.0\r\n";
                } else {
                    outputString = "GET " + "http://" + address + " HTTP/1.0\r\n";
                }

            }

            //for each key:value entry in headers, concatenate it to the output string
            if (headers != null) {
                if (!headers.isEmpty()) {
                    for (Map.Entry entry : headers.entrySet()) {
                        outputString += entry.getKey().toString() + ": " + entry.getValue() + "\r\n";
                    }
                }

            }
        }
        else
        {

            outputString = "GET " + path + " HTTP/1.0\r\n";
        }


        outputString += "\r\n";
        Object[] objectArray = new Object[2];
        objectArray[0] = outputString;
        objectArray[1] = 0;

        return objectArray;


    }

    public static String POST(String path, LinkedHashMap<String,String> headers, String body, String address, String query, boolean fileServer) throws Exception{

        String outputString = "";

        if (!fileServer) {

            if (query != null) {
                if (path != null) {
                    outputString = "POST " + "http://" + address + path + "?" + query + " HTTP/1.0\r\n";
                } else {
                    outputString = "POST " + "http://" + address + "?" + query + " HTTP/1.0\r\n";
                }
            } else {
                if (path != null) {
                    outputString = "POST " + "http://" + address + path + " HTTP/1.0\r\n";
                } else {
                    outputString = "POST " + "http://" + address + " HTTP/1.0\r\n";
                }

            }

            if (body != null) {
                outputString += "Content-Length: " + body.length() + "\r\n";
            } else {
                outputString += "Content-Length: 0\r\n";
            }

            if (headers != null) {
                if (!headers.isEmpty()) {
                    for (Map.Entry entry : headers.entrySet()) {
                        outputString += entry.getKey().toString() + ": " + entry.getValue() + "\r\n";
                    }
                }
            }
        }
        else
        {
            outputString = "POST " + path + " HTTP/1.0\r\n";
        }

        //send headers
        outputString += "\r\n" + body + "\r\n";

        return outputString;

    }


    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.accepts("router-host", "Router hostname")
                .withOptionalArg()
                .defaultsTo("localhost");

        parser.accepts("router-port", "Router port number")
                .withOptionalArg()
                .defaultsTo("3000");

        parser.accepts("server-host", "EchoServer hostname")
                .withOptionalArg()
                .defaultsTo("localhost");

        parser.accepts("server-port", "EchoServer listening port")
                .withOptionalArg()
                .defaultsTo("8007");

        OptionSet opts = parser.parse(args);

        // Router address
        String routerHost = (String) opts.valueOf("router-host");
        int routerPort = Integer.parseInt((String) opts.valueOf("router-port"));

        // Server address
        String serverHost = (String) opts.valueOf("server-host");
        int serverPort = Integer.parseInt((String) opts.valueOf("server-port"));

        SocketAddress routerAddress = new InetSocketAddress(routerHost, routerPort);
        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, serverPort);

        runClient(routerAddress, serverAddress);

    }

    public static Object[] createOutputPayload()
    {
        return new Object[2];
    }

}

