package main;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_READ;

public class UDPClient {

    int packetType = 0; //0 -> data, 1 -> SYN, 2 -> SYN-ACK, 3 - ACK 4- NAK

    private static final Logger logger = LoggerFactory.getLogger(UDPClient.class);

    public static long threeWayHandShake(SocketAddress routerAddr, InetSocketAddress serverAddr) throws Exception
    {
        try(DatagramChannel channel = DatagramChannel.open()){

            List<byte[]> bytelist = new ArrayList<>();
            long responseSequence = 0;
            //SENDING SYN

            Packet p = new Packet.Builder()
                    .setType(1)
                    .setSequenceNumber(0)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .create();
            channel.send(p.toBuffer(), routerAddr);


            logger.info("Sending SYN to router at {}" , routerAddr);

            // Try to receive a packet within timeout.
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            logger.info("Waiting for the response");
            selector.select(5000);

            Set<SelectionKey> keys = selector.selectedKeys();
            while (keys.isEmpty()){
                channel.send(p.toBuffer(), routerAddr);
                Thread.sleep(1000);
            }

            // We just want a single response.
            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
            SocketAddress router = channel.receive(buf);
            buf.flip();
            Packet resp = Packet.fromBuffer(buf);
//            logger.info("Packet: {}", resp);
//            logger.info("Router: {}", router);
            String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
//            logger.info("Payload: {}",  payload);

            if (resp.getType() != 2)
            {
                System.out.println("DID NOT RECEIVE A SYN-ACK");

            }
            else
            {
                System.out.println("SYN-ACK RECEIVED");
                responseSequence = resp.getSequenceNumber();
                System.out.println("Sequence Number from Server: " + resp.getSequenceNumber());

                keys.clear();
                channel.configureBlocking(true);

                Packet ACKPacket = new Packet.Builder()
                        .setType(3)
                        .setSequenceNumber(responseSequence + 1)
                        .setPortNumber(serverAddr.getPort())
                        .setPeerAddress(serverAddr.getAddress())
                        .create();

                channel.send(p.toBuffer(), routerAddr);
            }

            return(responseSequence + 1);
        }
    }

    public static void sendDataPacket(SocketAddress routerAddr, InetSocketAddress serverAddr, long sequenceNumber) throws IOException
    {
        try(DatagramChannel channel = DatagramChannel.open()){

            String outputPayload =  createOutputPayload();  //from get or post
            int packetType = 0; //0 -> data, 1 -> SYN, 2 -> SYN-ACK, 3 - ACK 4- NAK
            byte[] packetByteOutput = new byte[1013];
            List<byte[]> bytelist = new ArrayList<>();

            //sending data
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

            logger.info("Sending \"{}\" to router at {}", outputPayload, routerAddr);

        }
    }

    private static String recvPacket(DatagramChannel channel) throws IOException {

        // Try to receive a packet within timeout.
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_READ);
        logger.info("Waiting for the response");
        selector.select(5000);

        Set<SelectionKey> keys = selector.selectedKeys();
        if(keys.isEmpty()){
            logger.error("No response after timeout");
            return "";
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
        return payload;
    }

    public static Object[] createGET(String path, LinkedHashMap<String,String> headers, String address, String  query, boolean fileServer) throws Exception {

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

    public static String createPOST(String path, LinkedHashMap<String,String> headers, String body, String address, String query, boolean fileServer) throws Exception{

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

        long sequenceNumber = 0;
        try {
            sequenceNumber = threeWayHandShake(routerAddress, serverAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sendDataPacket(routerAddress, serverAddress, sequenceNumber);

    }

    public static String createOutputPayload()
    {
        return "";
    }

}

