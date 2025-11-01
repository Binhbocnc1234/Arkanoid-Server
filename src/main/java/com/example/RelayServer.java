package com.example;

import com.esotericsoftware.kryonet.*;
import com.esotericsoftware.kryo.Kryo;

public class RelayServer {
    private final Server server = new Server();
    private Connection playerA, playerB;

    public void start() throws Exception {
        Kryo kryo = server.getKryo();
        kryo.register(byte[].class);
        server.start();
        server.bind(54555, 54777);

        server.addListener(new Listener() {
            public void connected(Connection c) {
                System.out.println("Client connected: " + c.getID());
                if (playerA == null) playerA = c;
                else if (playerB == null) playerB = c;
            }

            public void disconnected(Connection c) {
                System.out.println("Client disconnected: " + c.getID());
                if (c == playerA) playerA = null;
                if (c == playerB) playerB = null;
            }

            public void received(Connection c, Object o) {
                if (!(o instanceof byte[] data)) return;
                Connection target = (c == playerA ? playerB : playerA);
                if (target != null) target.sendTCP(data);
            }
        });

        System.out.println("Relay server started on ports 54555/54777");
    }

    public static void main(String[] args) throws Exception {
        new RelayServer().start();
    }
}
