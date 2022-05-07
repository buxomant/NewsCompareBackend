package com.cbp.app.helper;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public class LoggingHelper {
    public static LocalTime logStartOfMethod(String methodName) {
        LocalTime startTime = LocalTime.now();
        System.out.println("[" + startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] <<< " + methodName + " [START] <<<");
        return startTime;
    }

    public static void logEndOfMethod(String methodName, LocalTime startTime) {
        LocalTime endTime = LocalTime.now();
        long durationInMillis = ChronoUnit.MILLIS.between(startTime, endTime);
        String formattedDuration = durationInMillis > 1000
            ? String.format("%ss %sms", durationInMillis / 1000, durationInMillis % 1000)
            : String.format("%sms", durationInMillis % 1000);
        System.out.printf(
            "[%s] >>> %s [%s] >>> %n",
            endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            methodName,
            formattedDuration
        );
    }

    public static void logMessage(String message) {
        LocalTime startTime = LocalTime.now();
        System.out.println("[" + startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] === " + message);
    }
}
