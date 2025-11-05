package com.example;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import com.example.messages.*;

public class RelayClient {
    private final Client client = new Client();
    private Listener listener;
    public interface RelayListener {
        void onAssigned(int id); // 0=rejected,1,2
        void onPeerDisconnected();
        void onStart();
        void onPeerJoined();
    }
    private RelayListener relayListener;

    public void start(String host, int tcpPort) throws Exception {
        // Register message classes with Kryo for serialization
        client.getKryo().register(AssignIdMessage.class);
        client.getKryo().register(StartMessage.class);
        client.getKryo().register(DisconnectMessage.class);
        client.getKryo().register(PeerJoinedMessage.class);
        client.getKryo().register(JoinMessage.class);
        client.getKryo().register(PositionMessage.class);
        
        listener = new Listener() {
            @Override
            public void received(Connection c, Object o) {
                if (o instanceof AssignIdMessage msg) {
                    System.out.println("Received: " + msg);
                    if (relayListener != null) relayListener.onAssigned(msg.id);
                } else if (o instanceof StartMessage msg) {
                    System.out.println("Received: " + msg);
                    if (relayListener != null) relayListener.onStart();
                } else if (o instanceof PeerJoinedMessage msg) {
                    System.out.println("Received: " + msg);
                    if (relayListener != null) relayListener.onPeerJoined();
                } else if (o instanceof DisconnectMessage msg) {
                    System.out.println("Received: " + msg);
                    if (relayListener != null) relayListener.onPeerDisconnected();
                } else if (o instanceof PositionMessage msg) {
                    // Position messages are handled elsewhere if needed
                    System.out.println("Received: " + msg);
                }
            }
        };
        client.addListener(listener);

        // Start the client's built-in update thread.
        // This runs in the background.
        client.start();
        
        // Try to connect. This call will block, but the background thread
        // (started by client.start()) will handle the handshake.
        try {
            client.connect(5000, host, tcpPort); // 5 second timeout
            System.out.println("Connected to relay in TCP-only mode!");
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            client.stop(); // Ensure client is stopped if connection fails
            throw e; // Re-throw exception so MultiplayerScene can catch it
        }
        
        // KHÔNG cần tự tạo và quản lý updateThread.
        // client.start() đã làm việc đó cho bạn.
        // Luồng nền sẽ tiếp tục chạy để nhận tin nhắn cho đến khi client.stop() được gọi.
    }

    public void sendPosition(float x, float y) {
        PositionMessage msg = new PositionMessage(x, y);
        client.sendTCP(msg);
    }

    public void setRelayListener(RelayListener l) {
        this.relayListener = l;
    }

    // Send a JOIN message to inform server we're here
    public void sendJoin() {
        JoinMessage msg = new JoinMessage();
        client.sendTCP(msg);
    }

    // Send START (only player1 will do this)
    public void sendStart() {
        StartMessage msg = new StartMessage();
        client.sendTCP(msg);
    }

    public void sendDisconnect() {
        DisconnectMessage msg = new DisconnectMessage();
        client.sendTCP(msg);
    }

    public void stop() {
        try {
            client.stop();
        } catch (Exception ignored) {}
    }

    // public static void main(String[] args) throws Exception {
    //     if (args.length == 0) {
    //         System.out.println("Usage: java RelayClient <relay_ip>");
    //         return;
    //     }
    //     RelayClient c = new RelayClient();
    //     c.start(args[0]);
    //     for (int i = 0; i < 5; i++) {
    //         c.sendPosition(i * 1.2f, i * 0.7f);
    //         Thread.sleep(1000);
    //     }
    // }
}
