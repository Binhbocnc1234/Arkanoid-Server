package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import com.example.messages.*;

public class RelayServer {
    private final Server server = new Server();
    private Connection playerA, playerB;

    public void start() throws Exception {
        // Register message classes with Kryo for serialization
        server.getKryo().register(AssignIdMessage.class);
        server.getKryo().register(StartMessage.class);
        server.getKryo().register(DisconnectMessage.class);
        server.getKryo().register(PeerJoinedMessage.class);
        server.getKryo().register(JoinMessage.class);
        server.getKryo().register(PositionMessage.class);
        
        server.start();
        // TCP-only mode for ngrok compatibility
        server.bind(54555);

        server.addListener(new Listener() {
            @Override
            public void connected(Connection c) {
                System.out.println("Client connected: " + c.getID());
                if (playerA == null) {
                    playerA = c;
                    // assign id 1
                    AssignIdMessage msg = new AssignIdMessage(1);
                    System.out.println("Sending: " + msg);
                    c.sendTCP(msg);
                }
                else if (playerB == null) {
                    playerB = c;
                    AssignIdMessage msg = new AssignIdMessage(2);
                    System.out.println("Sending: " + msg);
                    c.sendTCP(msg);
                    // inform playerA that playerB joined
                    if (playerA != null) {
                        PeerJoinedMessage peerMsg = new PeerJoinedMessage();
                        System.out.println("Sending to playerA: " + peerMsg);
                        playerA.sendTCP(peerMsg);
                    }
                }
                else {
                    // room full
                    AssignIdMessage msg = new AssignIdMessage(0);
                    System.out.println("Sending: " + msg + " (room full)");
                    c.sendTCP(msg);
                    c.close();
                }
            }

            @Override
            public void disconnected(Connection c) {
                System.out.println("Client disconnected: " + c.getID());
                if (c == playerA) {
                    playerA = null;
                    if (playerB != null) {
                        DisconnectMessage msg = new DisconnectMessage();
                        System.out.println("Sending to playerB: " + msg);
                        playerB.sendTCP(msg);
                    }
                }
                if (c == playerB) {
                    playerB = null;
                    if (playerA != null) {
                        DisconnectMessage msg = new DisconnectMessage();
                        System.out.println("Sending to playerA: " + msg);
                        playerA.sendTCP(msg);
                    }
                }
            }

            public void received(Connection c, Object o) {
                System.out.println("Received from " + c.getID() + ": " + o);
                
                // START message from player1 should be broadcast to both
                if (o instanceof StartMessage msg) {
                    if (playerA != null) {
                        System.out.println("Broadcasting START to playerA");
                        playerA.sendTCP(msg);
                    }
                    if (playerB != null) {
                        System.out.println("Broadcasting START to playerB");
                        playerB.sendTCP(msg);
                    }
                    return;
                }
                
                // DISCONNECT message forwarded to peer
                if (o instanceof DisconnectMessage msg) {
                    Connection target = (c == playerA ? playerB : playerA);
                    if (target != null) {
                        System.out.println("Forwarding DISCONNECT to peer");
                        target.sendTCP(msg);
                    }
                    return;
                }
                
                // JOIN message - just acknowledge (handled in connected())
                if (o instanceof JoinMessage) {
                    // Already handled in connected() callback
                    return;
                }
                
                // Default: forward all other messages (like PositionMessage) to peer
                Connection target = (c == playerA ? playerB : playerA);
                if (target != null) {
                    System.out.println("Forwarding to peer: " + o);
                    target.sendTCP(o);
                }
            }
        });

        // Thông báo cho chủ server
        String publicIP = getPublicIP();
        
        System.out.println("Public IP: " + publicIP);
        System.out.println("Relay server started on ports 54555");
    }
    public static String getPublicIP() {
        try {
            URL url = new URL("https://api.ipify.org");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
                return br.readLine();
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }
    public static void main(String[] args) throws Exception {
        new RelayServer().start();
    }
}

// new Thread(() -> {
//         try {
//             int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
//             com.sun.net.httpserver.HttpServer http =
//                 com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(port), 0);

//             http.createContext("/", exchange -> {
//                 String resp = "Relay server is alive.";
//                 exchange.sendResponseHeaders(200, resp.getBytes().length);
//                 exchange.getResponseBody().write(resp.getBytes());
//                 exchange.close();
//             });

//             http.start();
//             System.out.println("HTTP keep-alive running on port " + port);
//             } catch (Exception e) {
//                 e.printStackTrace();
//             }
//         }).start();