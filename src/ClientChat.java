import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientChat {

    private Socket socket;
    private String host;
    private int port;
    private String nickname;
    private PrintWriter output;
    private BufferedReader myInput;
    private BufferedReader chatInput;

    public static void main(String[] args) {
        ClientChat c1 = new ClientChat();
        c1.init();
    }

    public void init() {
        try {
            inputConnectionData();
            connectSocket();
            openStreams();
            ExecutorService t1 = Executors.newSingleThreadExecutor();
            t1.submit(new ChatReader());
            sendLoop();
        } catch (IOException e) {
            close();
        }
    }

    private void inputConnectionData() throws IOException {
        myInput = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Host : ");
        host = myInput.readLine();
        System.out.println("Port : ");
        port = Integer.parseInt(myInput.readLine());
        System.out.println("Choose nickname : ");
        nickname = myInput.readLine();
    }

    private void connectSocket() throws IOException {
        socket = new Socket(InetAddress.getByName(host), port);
        System.out.println("Connected : " + (socket.isConnected() ? "OK" : "FAIL"));
    }

    private void openStreams() throws IOException {
        output = new PrintWriter(socket.getOutputStream(), true);
        chatInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void sendLoop() throws IOException {
        while (socket.isConnected()) {
            String message = myInput.readLine();

            // special actions
            if (message.equals("/quit")) {
                output.println(nickname + " left the chat.");
                close();
            } else if (message.equals("/rename")) {
                setNickname();
                continue;
            }

            output.println(nickname + ": " + message);
        }
    }

    private void setNickname() throws IOException {
        System.out.println("Choose new nickname :");
        nickname = myInput.readLine();
    }

    private void close() {
        synchronized (socket) {
            try {
                output.close();
                myInput.close();
                chatInput.close();
                socket.close();
                System.out.println("Connection closed.");
                System.exit(1);
            } catch (IOException e) {
                System.out.println("Connection terminated.");
                System.exit(1);
            }
        }
    }

    private class ChatReader implements Runnable {

        @Override
        public void run() {
            try {
            while (!socket.isClosed()) {
                String message = chatInput.readLine();
                System.out.println(message);
                if (message.equals("You were kicked from the chat.")) {
                    close();
                }
            }
            } catch (IOException e) {
                close();
            }
        }
    }
}


