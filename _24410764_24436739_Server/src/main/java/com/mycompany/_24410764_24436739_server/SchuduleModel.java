package com.mycompany._24410764_24436739_server;

import java.util.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class SchuduleModel {
    private Map<String, Lecture> schedule = Collections.synchronizedMap(new HashMap<>());
    private Set<String> modules = Collections.synchronizedSet(new HashSet<>());

    public static class Lecture {
        String date;
        String time;
        String room;
        String module;

        Lecture(String date, String time, String room, String module) {
            this.date = date;
            this.time = time;
            this.room = room;
            this.module = module;
        }

        @Override
        public String toString() {
            return module + " in " + room;
        }
    }

    public synchronized String addLecture(String[] parts) throws IncorrectActionException {
        if (parts.length < 5) throw new IncorrectActionException("Invalid action");
        String date = parts[1];
        String time = parts[2];
        String room = parts[3];
        String module = parts[4];

        if (!modules.contains(module) && modules.size() >= 5) {
            return "ERROR|Module limit reached: maximum of 5 modules allowed per course";
        }

        String key = date + "-" + time;
        if (schedule.containsKey(key)) {
            return "ERROR|Time Clash: Lecture already exists at this time";
        }
        for (Lecture lecture : schedule.values()) {
            if (lecture.room.equals(room) && lecture.time.equals(time)) {
                return "ERROR|Room Clash: " + room + " is already book at " + time + " by " + lecture.module;
            }
        }
        Lecture lecture = new Lecture(date, time, room, module);
        schedule.put(key, lecture);
        modules.add(module);
        return "OK|Lecture added: " + module + " in " + room + " on " + date + " at " + time;
    }

    public synchronized String removeLecture(String[] parts) throws IncorrectActionException {
        if (parts.length < 3) throw new IncorrectActionException("REMOVE requires Date and Time");
        String date = parts[1];
        String time = parts[2];

        String key = date + "-" + time;
        if (schedule.remove(key) != null) {
            return "OK|Lecture removed";
        }
        return "ERROR|No Lecture found to be removed";
    }

    public synchronized String displaySchedule() {
        if (schedule.isEmpty()) {
            return "OK|No schedule found";
        }

        StringBuilder builder = new StringBuilder("OK|");
        for (Lecture lecture : schedule.values()) {
            builder.append(lecture.date)
                    .append(",")
                    .append(lecture.time)
                    .append(",")
                    .append(lecture.room)
                    .append(",")
                    .append(lecture.module)
                    .append(";");
        }
        return builder.toString();
    }

    public String earlyLectures() {
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new RecursiveAction() {
            @Override
            protected void compute() {
                invokeAll(
                        new DayShiftingTask("Monday"),
                        new DayShiftingTask("Tuesday"),
                        new DayShiftingTask("Wednesday"),
                        new DayShiftingTask("Thursday"),
                        new DayShiftingTask("Friday")
                );
            }
        });
        pool.shutdown();
        return displaySchedule();
    }

    private class DayShiftingTask extends RecursiveAction {
        private final String day;
        private final List<String> earlySlots = Arrays.asList(
                "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00"
        );

        DayShiftingTask(String day) {
            this.day = day;
        }

        @Override
        protected void compute() {
            List<Lecture> dayLectures = new ArrayList<>();
            List<String> dayKeys = new ArrayList<>();
            for (Map.Entry<String, Lecture> entry : schedule.entrySet()) {
                Lecture lecture = entry.getValue();
                LocalDate date = LocalDate.parse(lecture.date);
                String lectureDay = date.getDayOfWeek().getDisplayName(
                        TextStyle.FULL, Locale.ENGLISH
                );
                if (lectureDay.equals(day)) {
                    dayLectures.add(lecture);
                    dayKeys.add(entry.getKey());
                }
            }
            if (dayLectures.isEmpty()) return;
            dayLectures.sort(Comparator.comparing(l -> l.time));
            if (dayLectures.size() > earlySlots.size()) return;
            for (int i = 0; i < dayLectures.size(); i++) {
                String targetSlot = earlySlots.get(i);
                for (Lecture existing : schedule.values()) {
                    LocalDate date = LocalDate.parse(existing.date);
                    String existingDay = date.getDayOfWeek().getDisplayName(
                            TextStyle.FULL, Locale.ENGLISH
                    );
                    if (existingDay.equals(day) && existing.time.equals(targetSlot) && !dayLectures.contains(existing)) {
                        return;
                    }
                }
            }
            synchronized (SchuduleModel.this) {
                for (String key : dayKeys) {
                    schedule.remove(key);
                }
                for (int i = 0; i < dayLectures.size(); i++) {
                    Lecture old = dayLectures.get(i);
                    String newTime = earlySlots.get(i);
                    Lecture shifted = new Lecture(old.date, newTime, old.room, old.module);
                    String newKey = old.date + "-" + newTime;
                    schedule.put(newKey, shifted);
                }
            }
        }
    }

    public static class IncorrectActionException extends Exception {
        public IncorrectActionException(String message) {
            super(message);
        }
    }
}


