package Server.storage;

import Server.model.Reminder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ReminderStorage {
    private static final String FILE_PATH = "reminders.ser";

    public void saveReminders(List<Reminder> reminders) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(reminders);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Reminder> loadReminders() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<Reminder>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Add method to load reminders for a specific user
    public List<Reminder> loadRemindersForUser(String userName) {
        List<Reminder> allReminders = loadReminders();
        List<Reminder> userReminders = new ArrayList<>();
        for (Reminder reminder : allReminders) {
            if (reminder.getUserName().equals(userName)) {
                userReminders.add(reminder);
            }
        }
        return userReminders;
    }
}
