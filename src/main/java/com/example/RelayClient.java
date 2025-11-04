package com.example;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

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
        // Register byte[] class with Kryo for serialization
        client.getKryo().register(byte[].class);
        
        listener = new Listener() {
            @Override
            public void received(Connection c, Object o) {
                if (!(o instanceof byte[] data)) return;
                if (data.length < 1) return;
                int opcode = data[0] & 0xFF;
                switch (opcode) {
                    case 0x01: // ASSIGN_ID
                        int id = (data.length >= 2) ? (data[1] & 0xFF) : 0;
                        System.out.println("Assigned id: " + id);
                        if (relayListener != null) relayListener.onAssigned(id);
                        break;
                    case 0x02: // START
                        System.out.println("Start received");
                        if (relayListener != null) relayListener.onStart();
                        break;
                    case 0x06: // PEER_JOINED
                        System.out.println("Peer joined");
                        if (relayListener != null) relayListener.onPeerJoined();
                        break;
                    case 0x05: // DISCONNECT
                        System.out.println("Peer disconnected");
                        if (relayListener != null) relayListener.onPeerDisconnected();
                        break;
                    default:
                        // other opcodes (pos) handled elsewhere later
                        break;
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
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putFloat(x).putFloat(y);
        client.sendTCP(buf.array());
    }

    public void setRelayListener(RelayListener l) {
        this.relayListener = l;
    }

    // Send a JOIN message (opcode 0x10) to inform server we're here
    public void sendJoin() {
        byte[] msg = new byte[] {(byte)0x10};
        client.sendTCP(msg);
    }

    // Send START (only player1 will do this)
    public void sendStart() {
        byte[] msg = new byte[] {(byte)0x02};
        client.sendTCP(msg);
    }

    public void sendDisconnect() {
        byte[] msg = new byte[] {(byte)0x05};
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
