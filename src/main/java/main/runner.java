package main;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashMap;



public class runner {
    public static void main(String[] args) {
        //manually start router


//        //    initiliaze ports
//        SocketAddress routerAddress = new InetSocketAddress("192.168.2.10", 3000);
//        InetSocketAddress serverAddress = new InetSocketAddress("192.168.2.3", 8007);
//        InetSocketAddress clientAddress = new InetSocketAddress("192.168.2.125", 41830);

//        //    3 way dick shake
//        //threeWayHandShake(SocketAddress routerAddr, InetSocketAddress serverAddr, long numberOfPackets)
//        UDPClient.threeWayHandShake(routerAddress, serverAddress, 10);

        //    get -v -h headerKey=headerValue http://localhost:80/get?course=networking&assignment=1                                                                     FIX

        //    create GET request, send with send DataPacket()
        //                sendDataPacket(createGet(args))

        //createGET(String path, LinkedHashMap<String,String> headers, String address, String  query, boolean fileServer)
    }
}
