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


    private static final Logger logger = LoggerFactory.getLogger(UDPClient.class);

    public static long threeWayHandShake(SocketAddress routerAddr, InetSocketAddress serverAddr, String numberOfPackets) throws Exception
    {
        try(DatagramChannel channel = DatagramChannel.open()){

            long responseSequence = 0;
            //SENDING SYN

            Packet SYNPack = new Packet.Builder()
                    .setType(1)
                    .setSequenceNumber(0)
                    .setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress())
                    .setPayload(new byte[0])
                    .create();
            channel.send(SYNPack.toBuffer(), routerAddr);


            logger.info("Sending SYN to router at {}" , routerAddr);

            // Try to receive a packet within timeout.
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            logger.info("Waiting for the response");
            selector.select(5000);

            Set<SelectionKey> keys = selector.selectedKeys();
            while (keys.isEmpty()){
                channel.send(SYNPack.toBuffer(), routerAddr);
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
                logger.info("DID NOT RECEIVE A SYN-ACK");

            }
            else
            {
                logger.info("SYN-ACK RECEIVED");
                responseSequence = resp.getSequenceNumber();
                logger.info("Sequence Number from Server: " + resp.getSequenceNumber());


                keys.clear();


                Packet ACKPacket = new Packet.Builder()
                        .setType(3)
                        .setSequenceNumber(responseSequence + 1)
                        .setPortNumber(serverAddr.getPort())
                        .setPeerAddress(serverAddr.getAddress())
                        .setPayload(numberOfPackets.getBytes())
                        .create();

                channel.send(ACKPacket.toBuffer(), routerAddr);
            }

            return(responseSequence + 1);
        }
    }

    public static void sendDataPacket(SocketAddress routerAddr, InetSocketAddress serverAddr, List<byte[]> outputByteList) throws IOException {
        try(DatagramChannel channel = DatagramChannel.open()){

            long sequenceNumber = 1;
            int packetType = 0; //0 -> data, 1 -> SYN, 2 -> SYN-ACK, 3 - ACK 4- NAK
            int index = 0;
            List<Packet> packetList = new ArrayList();
            List<Packet> window = new ArrayList();
            int maxWindowSize = 2;
            int availableWindowSize = 2;

            //while not done sending
            while (true) {

                //create a list of packets to send
                for (byte[] b : outputByteList) {

                    packetList.add(new Packet.Builder()
                            .setType(packetType)
                            .setSequenceNumber(sequenceNumber)
                            .setPortNumber(serverAddr.getPort())
                            .setPeerAddress(serverAddr.getAddress())
                            .setPayload(b)
                            .create());
                    sequenceNumber++;
                }

                //load packets in window
                while (availableWindowSize > 0) {
                    window.add(packetList.get(index));
                    index++;
                    availableWindowSize--;
                }


                //send all the packets in window that haven't been sent before
                for (Packet p : window) {
                    if (!p.isSent())
                    {
                        channel.send(p.toBuffer(), routerAddr);
                        p.setSent(true);
                    }
                }

                logger.info("Sending packet sequence number \"{}\" to router at {}", sequenceNumber, routerAddr);
                logger.info("Waiting for the response");

                Selector selector = Selector.open();
                channel.configureBlocking(false);
                channel.register(selector, OP_READ);
                selector.select(5000);
                Set<SelectionKey> keys = selector.selectedKeys();

                // if no response came thru, send all again and wait 5 sec
                while (keys.isEmpty()) {
                    for (Packet p : window) {
                        channel.send(p.toBuffer(), routerAddr);
                    }
                    selector.select(5000);
                }

                //TODO understand if we analyze them one at a time
                //get ACK for first data packet and remove from the unacked list
                Packet responsePacket = recvPacket(channel);

                //if the first unacked number is acked, remove it and add a spot on the window
                if (responsePacket.getSequenceNumber() == window.get(0).getSequenceNumber())
                {
                    window.remove(window.get(0));
                    availableWindowSize = maxWindowSize - window.size();
                }

                // if received sequence number is not the first unacked number, remove the packet but dont increase window size
                window.removeIf(p -> responsePacket.getSequenceNumber() == p.getSequenceNumber());


            }

        }
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

    public static List<byte[]> createGET(String path, LinkedHashMap<String,String> headers, String address, String  query, boolean fileServer) throws Exception {

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


        byte[] packetByteOutput = new byte[1013];
        List<byte[]> bytelist = new ArrayList<>();

        //sending data
        byte[] byteArray = outputString.getBytes();
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

    public static List<byte[]> createPOST(String path, LinkedHashMap<String,String> headers, String body, String address, String query, boolean fileServer) throws Exception{

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

        byte[] packetByteOutput = new byte[1013];
        List<byte[]> bytelist = new ArrayList<>();

        //sending data
        byte[] byteArray = outputString.getBytes();
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

        return bytelist;

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

//        long sequenceNumber = 0;
//        List<byte[]> bytelist = createGET();
//        long numberOfPackets = createGET().size();
//
//
//        try {
//            sequenceNumber = threeWayHandShake(routerAddress, serverAddress, numberOfPackets);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        sendDataPacket(routerAddress, serverAddress, bytelist);

    }

    public static String createOutputPayload()
    {
        return "";
    }

}

