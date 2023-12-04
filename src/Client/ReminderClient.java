package Client;

import Server.model.Reminder;
import Server.model.User;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Scanner;
import javax.net.ssl.*;
import java.io.FileInputStream;

public class ReminderClient {
    private String host;
    private int port;
    private volatile boolean isRunning = true;

    public ReminderClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start(){

        System.setProperty("javax.net.ssl.trustStore", "src/SSL_Key/mykeystore2.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "faranzafar");

        SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try (SSLSocket socket = (SSLSocket) ssf.createSocket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.print("Enter your name: ");
            String name = scanner.nextLine();
            User user = new User(name);
            oos.writeObject(user);

            // Starting a thread to listen for messages from the server
            new Thread(() -> listenForServerMessages(ois)).start();

            while (isRunning) {
                System.out.println("Enter a reminder description or type 'exit' to quit:");
                String description = scanner.nextLine();

                if ("exit".equalsIgnoreCase(description)) {
                    isRunning = false;
                    break;
                }

                processReminderInput(description, scanner, oos, name);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForServerMessages(ObjectInputStream ois) {
        try {
            while (isRunning) {
                Object obj = ois.readObject();
                if (obj instanceof String) {
                    // Handle server notifications
                    System.out.println("Notification from server: " + obj);
                } else if (obj instanceof List) {
                    // Handle list of reminders
                    displayExistingReminders((List<Reminder>) obj);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Disconnected from server.");
        }
    }

    private void processReminderInput(String description, Scanner scanner, ObjectOutputStream oos, String userName) throws IOException {
        System.out.print("Enter year: ");
        int year = scanner.nextInt();

        System.out.print("Enter month (1-12): ");
        int month = scanner.nextInt();

        System.out.print("Enter day (1-31): ");
        int day = scanner.nextInt();

        System.out.print("Enter hour (0-23): ");
        int hour = scanner.nextInt();

        System.out.print("Enter minute (0-59): ");
        int minute = scanner.nextInt();
        scanner.nextLine(); // Consume the remaining newline

        LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute);
        Reminder reminder = new Reminder(1, userName, description, dateTime); // ID is set to 1 for simplicity
        oos.writeObject(reminder);
    }

    private void displayExistingReminders(List<Reminder> existingReminders) {
        if (existingReminders.isEmpty()) {
            System.out.println("You have no existing reminders.");
        } else {
            System.out.println("Your existing reminders:");
            for (Reminder r : existingReminders) {
                System.out.println("Reminder: " + r.getTaskDescription() + " - Due: " + r.getDateTime());
            }
        }
    }

    public static void main(String[] args)  {


        String host = "localhost";
        int port = 1234;
        ReminderClient client = new ReminderClient(host, port);
        client.start();
    }
}
