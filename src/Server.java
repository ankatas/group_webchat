import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final static int PORT = 7777;
    private ServerSocket serverSocket;
    private LinkedList<ChatManager> managersList;

    public static void main(String[] args) {
        Server s1 = new Server();
        s1.init();
    }

    public void init() {
        try {
            serverInit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void serverInit() throws IOException {
        serverSocket = new ServerSocket(PORT);
        managersList = new LinkedList<>();
        ExecutorService threadPool = Executors.newCachedThreadPool();
        System.out.println("Chat server created. Ready to accept connections.");
        while (true) {
            managersList.add(new ChatManager(serverSocket.accept()));
            threadPool.submit(managersList.getLast());
        }
    }

    private synchronized void sendAll(String message) {
        for (ChatManager manager : managersList) {
            manager.send(message);
        }
    }

    private String listAll() {
        String list = "\nThis chat has the following users active: \n";
        for (ChatManager manager : managersList) {
            list += manager.nickname + "\n";
        }
        return list;
    }

    private void sendWhisper(String recipient, String message) {
        for (ChatManager manager : managersList) {
            if (manager.nickname.equals(recipient)) {
                manager.send(message);
            }
        }
    }

    private void kick(String recipient) {
        for (ChatManager manager : managersList) {
            if (manager.nickname.equals(recipient)) {
                manager.send("You were kicked from the chat.");
                manager.close();
            }
        }
    }

    private class ChatManager implements Runnable {

        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ChatManager(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println(socket.isConnected() ? "New connection established." : "New connection failed.");
            try {
                setStreams();
                listen();
            } catch (IOException e) {
                close();
            }
        }

        private void setStreams() throws IOException {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        }

        private void listen() throws IOException {
            while (!socket.isClosed()) {
                String message = in.readLine();
                nickname = message.split(": ")[0];

                // special actions
                if (message.contains(" left the chat.")) {
                    close();
                    System.out.println(message);
                    sendAll(message);
                    return;
                } else if (message.contains("/list")) {
                    out.println(listAll());
                    continue;
                } else if (message.contains("/whisper")) {
                    String recipient = message.split(" ")[2];
                    sendWhisper(recipient, message);
                    send(message);
                    System.out.println(message);
                    continue;
                } else if (message.contains("/kick")) {
                    String recipient = message.split(" ")[2];
                    kick(recipient);
                    System.out.println(recipient + " was kicked from the chat.");
                    sendAll(recipient + " was kicked from the chat.");
                    continue;
                }

                System.out.println(message);
                sendAll(message);
            }
        }

        private void send(String message) {
            out.println(message);
        }

        private void close() {
            try {
                in.close();
                out.close();
                socket.close();
                managersList.remove(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
