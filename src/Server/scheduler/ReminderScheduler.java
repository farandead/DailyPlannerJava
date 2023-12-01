package Server.scheduler;

import Server.model.Reminder;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.*;

public class ReminderScheduler {
    private final ScheduledExecutorService scheduler;
    private Map<String, ObjectOutputStream> clientStreams; // Map user names to their streams

    public ReminderScheduler(Map<String, ObjectOutputStream> clientStreams) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.clientStreams = clientStreams;
    }

    public void scheduleReminder(Reminder reminder) {
        long delay = calculateDelay(reminder.getDateTime());
        scheduler.schedule(() -> {
            try {
                ObjectOutputStream oos = clientStreams.get(reminder.getUserName());
                if (oos != null) {
                    oos.writeObject("Reminder:  " + reminder.getTaskDescription());

                    // Log when a reminder is sent
                    System.out.println("Reminder sent to " + reminder.getUserName() + ": " + reminder.getTaskDescription() + " at  " + reminder.getDateTime());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * Schedules a reminder.
     * @param reminder The reminder to be scheduled.
     */


    /**
     * Calculates the delay in seconds until the reminder is due.
     * @param reminderTime The time at which the reminder is scheduled.
     * @return The delay in seconds.
     */
    private long calculateDelay(LocalDateTime reminderTime) {
        LocalDateTime now = LocalDateTime.now();
        return now.until(reminderTime, ChronoUnit.SECONDS);
    }

    /**
     * Shuts down the scheduler gracefully.
     */
    public void shutdown() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}