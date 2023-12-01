package Server;

import Server.model.Reminder;
import Server.model.User;
import Server.scheduler.ReminderScheduler;
import Server.storage.ReminderStorage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class ClientHandler extends Thread {
    private Socket clientSocket;
    private ReminderStorage storage;
    private ReminderScheduler scheduler;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private ConcurrentHashMap<String, ObjectOutputStream> clientStreams;
    private static final int MAX_REQUESTS_PER_MINUTE = 10; // Example limit
    private static final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    public ClientHandler(Socket socket, ReminderStorage storage, ReminderScheduler scheduler, ConcurrentHashMap<String, ObjectOutputStream> clientStreams) {
        this.clientSocket = socket;
        this.storage = storage;
        this.scheduler = scheduler;
        this.clientStreams = clientStreams;
    }

    @Override
    public void run() {

        User user;
        try {
            ois = new ObjectInputStream(clientSocket.getInputStream());
            oos = new ObjectOutputStream(clientSocket.getOutputStream());

            user = (User) ois.readObject();
            String clientIP = clientSocket.getInetAddress().getHostAddress();
            System.out.println("Client connected: " + user.getName());
            clientStreams.put(user.getName(), oos);

            if (isRateLimitExceeded(clientIP)) {
                System.out.println("Rate limit exceeded for IP: " + clientIP);
                clientSocket.close();
                return;
            }

            while (true) {
                Object obj = ois.readObject();
                if (obj instanceof Reminder) {
                    Reminder reminder = (Reminder) obj;
                    processNewReminder(reminder);
                    sendUserReminders(user.getName());
                    sendConfirmationMessage(user.getName(), "Reminder set for: " + reminder.getDateTime());
                } else if ("exit".equals(obj)) {
                    break; // Exit the loop if an "exit" command is received
                }
            }

        } catch (EOFException | SocketException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                user = (User) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            cleanup(user.getName());
        }
    }
    private boolean isRateLimitExceeded(String ip) {
        long currentTime = System.currentTimeMillis();
        lastRequestTime.putIfAbsent(ip, currentTime);
        requestCounts.putIfAbsent(ip, new AtomicInteger(0));

        long timeSinceLastRequest = currentTime - lastRequestTime.get(ip);
        AtomicInteger requestCount = requestCounts.get(ip);

        if (timeSinceLastRequest > TimeUnit.MINUTES.toMillis(1)) {
            requestCount.set(1);
            lastRequestTime.put(ip, currentTime);
            return false;
        } else {
            return requestCount.incrementAndGet() > MAX_REQUESTS_PER_MINUTE;
        }
    }

    private void processNewReminder(Reminder reminder) {
        List<Reminder> reminders = storage.loadReminders();
        reminders.add(reminder);
        storage.saveReminders(reminders);
        scheduler.scheduleReminder(reminder);
        System.out.println("New reminder added for " + reminder.getUserName() + ": " + reminder.getTaskDescription() + " at " + reminder.getDateTime());

    }

    private void sendUserReminders(String userName) throws IOException {
        List<Reminder> userReminders = getUserReminders(userName);
        oos.writeObject(userReminders);
    }

    private void sendConfirmationMessage(String userName, String message) throws IOException {
        ObjectOutputStream userStream = clientStreams.get(userName);
        if (userStream != null) {
            userStream.writeObject(message);
        }
    }

    private List<Reminder> getUserReminders(String userName) {
        return storage.loadReminders().stream()
                .filter(reminder -> reminder.getUserName().equals(userName))
                .collect(Collectors.toList());
    }

    private void cleanup(String userName) {
        try {
            clientStreams.remove(userName);
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}