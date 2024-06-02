import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;

public class Client {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 1234);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Generate a unique identifier for this client
            String uniqueID = UUID.randomUUID().toString();

            // Send a simple HTTP GET request with a unique identifier
            out.println("GET /index.html HTTP/1.0");
            out.println("Host: localhost");
            out.println("X-Unique-ID: " + uniqueID);
            out.println("");

            // Read and print the response from the server
            String response;
            while ((response = in.readLine()) != null) {
                System.out.println(response);
            }

            in.close();
            out.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
