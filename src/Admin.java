import java.util.Scanner;

public class Admin {
    private Server server;

    // Constructor that accepts Server object
    public Admin(Server server) {
        this.server = server;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        String command;

        while (true) {
            System.out.print("Enter command (start/shutdown/list): ");
            command = scanner.nextLine();

            if (command.equalsIgnoreCase("start")) {
                new Thread(() -> server.start()).start();
            } else if (command.equalsIgnoreCase("shutdown")) {
                server.stop();
                break;
            } else if (command.equalsIgnoreCase("list")) {
                server.listActiveConnections();
            } else {
                System.out.println("Unknown command.");
            }
        }

        scanner.close();
    }

    public static void main(String[] args) {
        Server server = new Server(1234); // Create Server instance with desired port
        Admin admin = new Admin(server); // Pass Server instance to Admin constructor
        admin.run(); // Start the Admin CLI
    }
}
