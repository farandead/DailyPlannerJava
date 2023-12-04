package Server;

import Server.model.Reminder;
import Server.storage.ReminderStorage;
import Server.scheduler.ReminderScheduler;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.*;



public class PlannerServer {
    private SSLServerSocket serverSocket;
    private ReminderStorage storage;
    private ReminderScheduler scheduler;
    private ConcurrentHashMap<String, ObjectOutputStream> clientStreams;

    public PlannerServer(int port) throws IOException {
//
        System.setProperty("javax.net.ssl.keyStore", "src/SSL_Key/mykeystore2.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "faranzafar");

        SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        serverSocket = (SSLServerSocket) ssf.createServerSocket(port);
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