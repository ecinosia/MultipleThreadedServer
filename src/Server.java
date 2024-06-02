import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private final ReentrantLock lock = new ReentrantLock(); // Create a lock
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private List<ClientHandler> activeClients;
    private volatile boolean running;

    public static void main(String[] args) {
        Server server = new Server(1234);
        Admin admin = new Admin(server);
        admin.run();
    }

    private void s(String s2) { // an alias to avoid typing so much!
        System.out.println(s2);
    }

    private int port;

    public Server(int listen_port) {
        port = listen_port;
        activeClients = new ArrayList<>();
    }

    public void start() {
        try {
            s("Trying to bind to localhost on port " + port + "...");
            serverSocket = new ServerSocket(port);
            executor = Executors.newCachedThreadPool();
            running = true;

            while (running) {
                s("\nReady, Waiting for requests...\n");
                try {
                    Socket connectionSocket = serverSocket.accept();
                    if (!running) break;

                    InetAddress client = connectionSocket.getInetAddress();
                    s(client.getHostName() + " connected to server.\n");

                    ClientHandler clientHandler = new ClientHandler(connectionSocket);
                    activeClients.add(clientHandler);
                    executor.submit(clientHandler);
                } catch (Exception e) {
                    if (running) s("\nError:" + e.getMessage());
                }
            }
        } catch (Exception e) {
            s("\nFatal Error:" + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
            executor.shutdownNow();
            for (ClientHandler client : activeClients) {
                client.stop();
            }
        } catch (Exception e) {
            s("Error while stopping the server: " + e.getMessage());
        }
    }

    public void listActiveConnections() {
        for (ClientHandler client : activeClients) {
            s("Active client: " + client.getClientInfo());
        }
    }

    private class ClientHandler implements Runnable {
        private Socket connectionSocket;
        private BufferedReader input;
        private DataOutputStream output;

        public ClientHandler(Socket connectionSocket) {
            this.connectionSocket = connectionSocket;
            running = true;
        }

        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                output = new DataOutputStream(connectionSocket.getOutputStream());
                httpHandler(input, output);
            } catch (Exception e) {
                s("\nError:" + e.getMessage());
            } finally {
                try {
                    input.close();
                    output.close();
                    connectionSocket.close();
                } catch (Exception e) {
                    s("Error closing client connection: " + e.getMessage());
                }
                activeClients.remove(this);
            }
        }

        public void stop() {
            running = false;
            try {
                connectionSocket.close();
            } catch (Exception e) {
                s("Error stopping client handler: " + e.getMessage());
            }
        }

        public String getClientInfo() {
            return connectionSocket.getInetAddress().getHostName();
        }

        @SuppressWarnings("unused")
        private void httpHandler(BufferedReader input, DataOutputStream output) {
            int method = 0; // 1 get, 2 head, 0 not supported
            String path = ""; // what path
            try {
                String tmp = input.readLine(); // read from the stream
                System.out.println("read: " + tmp);
                String tmp2 = new String(tmp);

                tmp = tmp.toUpperCase(); // convert it to uppercase
                if (tmp.startsWith("GET")) { // compare it is it GET
                    method = 1;
                } // if we set it to method 1
                if (tmp.startsWith("HEAD")) { // same here is it HEAD
                    method = 2;
                } // set method to 2

                int start = 0;
                int end = 0;
                for (int a = 0; a < tmp2.length(); a++) {
                    if (tmp2.charAt(a) == ' ' && start != 0) {
                        end = a;
                        break;
                    }
                    if (tmp2.charAt(a) == ' ' && start == 0) {
                        start = a;
                    }
                }
                path = tmp2.substring(start + 2, end); // fill in the path

                String line;
                String uniqueID = "Unknown";
                while (!(line = input.readLine()).isEmpty()) {
                    if (line.startsWith("X-Unique-ID:")) {
                        uniqueID = line.split(": ")[1];
                    }
                }

                s("Opening file: " + path + " for client: " + uniqueID);

                lock.lock(); // Acquire the lock before accessing the file
                s("The thread has been locked until the " + uniqueID + " complete its request.");

                try {
                    output.writeBytes(constructHttpHeader(200, 5));

                    BufferedReader br = new BufferedReader(new FileReader(new File(path)));
                    String fileLine;
                    while ((fileLine = br.readLine()) != null) {
                        output.writeUTF(fileLine);
                        s("line: " + fileLine);
                    }
                    output.writeUTF("Requested file name: " + path);
                    output.writeUTF("Hello world");

                    br.close();
                } finally {
                    Thread.sleep(15000);
                    lock.unlock(); // Ensure the lock is released after file access
                    s("The thread has been unlocked.");
                }

                output.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String constructHttpHeader(int returnCode, int fileType) {
            String s = "HTTP/1.0 ";
            switch (returnCode) {
                case 200:
                    s = s + "200 OK";
                    break;
                case 400:
                    s = s + "400 Bad Request";
                    break;
                case 403:
                    s = s + "403 Forbidden";
                    break;
                case 404:
                    s = s + "404 Not Found";
                    break;
                case 500:
                    s = s + "500 Internal Server Error";
                    break;
                case 501:
                    s = s + "501 Not Implemented";
                    break;
            }

            s = s + "\r\n";
            s = s + "Connection: close\r\n";
            s = s + "Server: SmithOperatingSystemsCourse v0\r\n"; // server name

            switch (fileType) {
                case 0:
                    break;
                case 1:
                    s = s + "Content-Type: image/jpeg\r\n";
                    break;
                case 2:
                    s = s + "Content-Type: image/gif\r\n";
                case 3:
                    s = s + "Content-Type: application/x-zip-compressed\r\n";
                default:
                    s = s + "Content-Type: text/html\r\n";
                    break;
            }
            s = s + "\r\n";
            return s;
        }
    }
}
