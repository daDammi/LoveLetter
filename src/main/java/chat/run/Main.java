package chat.run;

import chat.client.Client;
import chat.server.Server;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar vp-damboeck-1.0-SNAPSHOT.jar <server|client>");
            return;
        }

        String mode = args[0];
        if (mode.equalsIgnoreCase("server")) {
            Server server = new Server();
            server.runServer();
        } else if (mode.equalsIgnoreCase("client")) {
            Client client = new Client();
            client.startClient();
        } else {
            System.out.println("Use either 'server' or 'client' as argument.");
        }
    }
}
