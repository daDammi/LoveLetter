package chat.client;

import chat.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Client class for creating new clients for the associated server.
 * Create client object and start a new client with the .startClient method.
 */
public class Client {
    public Socket socket;
    private static BufferedReader input;
    private PrintWriter output;
    private boolean done;

    /**
     * Method for starting a new client from a Client object.
     */
    public void startClient() {
        try {
            socket = new Socket("localhost", Server.PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            //run a new thread with an inputHandler
            InputHandler inputHandler = new InputHandler();
            Thread t = new Thread(inputHandler);
            t.start();

            String inMessage;
            while ((inMessage = input.readLine()) != null) {
                System.out.println(inMessage);
            }

        } catch (IOException e) {
            System.err.println("Server is closed!");
        } finally {
            try {
                input.close();
                output.close();
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class InputHandler implements Runnable {
        @Override
        public void run() {
            try {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while (!done) {
                    String message = inReader.readLine();
                    if (message.equals("bye")) {
                        if (!Server.gameRunning) {
                            output.println(message);
                            inReader.close();
                            done = true;
                            try {
                                input.close();
                                output.close();
                                if (!socket.isClosed()) {
                                    socket.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        output.println(message);
                    }
                }
            } catch (IOException e) {
                done = true;
                try {
                    input.close();
                    output.close();
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException f) {
                    f.printStackTrace();
                }
            }
        }
    }
}
