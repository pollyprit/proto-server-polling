import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;

public class TCPClient {
    public static long conns = 0;
    public static void main(String[] args) {


        int count = 200;
        Thread[] ts = new Thread[count];

        for (int i = 0; i < count; ++i)
            ts[i] = new Thread(new Connector());

        for (int i = 0; i < count; ++i)
            ts[i].start();

        for (int i = 0; i < count; ++i) {
            try {
                ts[i].join();
            } catch (InterruptedException e) {
            }
        }
    }


    public static class Connector extends Thread {
        @Override
        public void run() {
            String serverAddress = "daebroker-rhel8";
            int port = 6849;

            for (int i = 0; i < 1000; ++i) {
                try (Socket socket = new Socket(serverAddress, port);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    ++conns;
                    System.out.println("Connection created: " + conns);

                    // Send a message to the server
                    String message = "Hello from client";
                    out.println(message);
                    /*System.out.println("Message sent: " + message);

                    // Read the response from the server
                    String response = in.readLine();
                    System.out.println("Server response: " + response);*/
                } catch (IOException e) {
                }
            }
        }
    }

}
