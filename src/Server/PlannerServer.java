package Server;

import Server.model.Reminder;
import Server.storage.ReminderStorage;
import Server.scheduler.ReminderScheduler;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PlannerServer {
    private ServerSocket serverSocket;
    private ReminderStorage storage;
    private ReminderScheduler scheduler;
    private ConcurrentHashMap<String, ObjectOutputStream> clientStreams;

    public PlannerServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        storage = new ReminderStorage();
        clientStreams = new ConcurrentHashMap<>(); // Initialize the map
        scheduler = new ReminderScheduler(clientStreams);

        // Load and schedule existing reminders
        List<Reminder> existingReminders = storage.loadReminders();
        for (Reminder reminder : existingReminders) {
            scheduler.scheduleReminder(reminder); // Schedule each reminder individually
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            System.out.println("Server shutdown gracefully.");
        }));
    }


    public void start() throws IOException {
        System.out.println("Server Running ......");
        while (true) {
            // Pass the clientStreams map to each new ClientHandler
            new ClientHandler(serverSocket.accept(), storage, scheduler, clientStreams).start();
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 1234; // Example port number
        PlannerServer server = new PlannerServer(port);
        server.start();
    }
}