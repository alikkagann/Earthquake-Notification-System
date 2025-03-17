import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Comparator;

public class EarthquakeNotification {

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: java EarthquakeNotification [--all] <watcherFile> <earthquakeFile>");
            return;
        }

        boolean printAll = false;
        String watcherFileName;
        String earthquakeFileName;

        if (args.length == 3) {
            if (args[0].equals("--all")) {
                printAll = true;
                watcherFileName = args[1];
                earthquakeFileName = args[2];
            } else {
                System.out.println("Usage: java EarthquakeNotification [--all] <watcherFile> <earthquakeFile>");
                return;
            }
        } else {
            watcherFileName = args[0];
            earthquakeFileName = args[1];
        }

        List<WatcherEvent> watcherEvents = readWatcherFile(watcherFileName);
        List<EarthquakeEvent> earthquakeEvents = readEarthquakeFile(earthquakeFileName);

        List<Event> events = new ArrayList<>();

        for (int i = 0; i < watcherEvents.size(); i++) {
            events.add(events.size(), watcherEvents.get(i));
        }

        for (int i = 0; i < earthquakeEvents.size(); i++) {
            events.add(events.size(), earthquakeEvents.get(i));
        }

        sortEventsByTime(events);

        simulateEvents(events, printAll);
    }

    public static void sortEventsByTime(List<Event> events) {
        int n = events.size();
        for (int i = 1; i < n; ++i) {
            Event key = events.get(i);
            int j = i - 1;
            while (j >= 0 && events.get(j).getTime() > key.getTime()) {
                events.set(j + 1, events.get(j));
                j = j - 1;
            }
            events.set(j + 1, key);
        }
    }

    public static List<WatcherEvent> readWatcherFile(String fileName) {
        List<WatcherEvent> watcherEvents = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim().replaceAll("\\s+", " ");
                if (line.isEmpty()) continue;

                String[] parts = line.split(" ");
                int time = Integer.parseInt(parts[0]);
                String action = parts[1];

                if (action.equals("add")) {
                    double longitude = Double.parseDouble(parts[2]);
                    double latitude = Double.parseDouble(parts[3]);
                    String name = parts[4];
                    watcherEvents.add(watcherEvents.size(), new WatcherEvent(time, action, longitude, latitude, name));
                } else if (action.equals("delete")) {
                    String name = parts[2];
                    watcherEvents.add(watcherEvents.size(), new WatcherEvent(time, action, name));
                } else if (action.equals("query-largest")) {
                    watcherEvents.add(watcherEvents.size(), new WatcherEvent(time, action));
                } else {
                    System.out.println("Unknown action: " + action);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading watcher file: " + e.getMessage());
            System.exit(1);
        }

        return watcherEvents;
    }

    public static List<EarthquakeEvent> readEarthquakeFile(String fileName) {
        List<EarthquakeEvent> earthquakeEvents = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            EarthquakeEvent currentEvent = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("<earthquake>")) {
                    currentEvent = new EarthquakeEvent();
                } else if (line.startsWith("<id>")) {
                    String id = line.replaceAll("<[^>]+>", "").trim();
                    currentEvent.id = id;
                } else if (line.startsWith("<time>")) {
                    String timeStr = line.replaceAll("<[^>]+>", "").trim();
                    int time = Integer.parseInt(timeStr);
                    currentEvent.time = time;
                } else if (line.startsWith("<place>")) {
                    String place = line.replaceAll("<[^>]+>", "").trim();
                    currentEvent.place = place;
                } else if (line.startsWith("<coordinates>")) {
                    String coords = line.replaceAll("<[^>]+>", "").trim();
                    String[] coordParts = coords.split(",");
                    double longitude = Double.parseDouble(coordParts[0].trim());
                    double latitude = Double.parseDouble(coordParts[1].trim());
                    double depth = Double.parseDouble(coordParts[2].trim());
                    currentEvent.longitude = longitude;
                    currentEvent.latitude = latitude;
                    currentEvent.depth = depth;
                } else if (line.startsWith("<magnitude>")) {
                    String magStr = line.replaceAll("<[^>]+>", "").trim();
                    double magnitude = Double.parseDouble(magStr);
                    currentEvent.magnitude = magnitude;
                } else if (line.startsWith("</earthquake>")) {
                    if (currentEvent != null) {
                        earthquakeEvents.add(earthquakeEvents.size(), currentEvent);
                        currentEvent = null;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading earthquake file: " + e.getMessage());
            System.exit(1);
        }
        return earthquakeEvents;
    }

    public static void simulateEvents(List<Event> events, boolean printAll) {
        KDTree2D watcherTree = new KDTree2D();

        LinkedList<EarthquakeEvent> earthquakeQueue = new LinkedList<>();
        MaxHeap<EarthquakeEvent> magnitudeHeap = new MaxHeap<>(new Comparator<EarthquakeEvent>() {
            @Override
            public int compare(EarthquakeEvent o1, EarthquakeEvent o2) {
                return Double.compare(o2.magnitude, o1.magnitude);
            }
        });

        int currentTime = 0;

        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            currentTime = event.getTime();

            removeOldEarthquakes(earthquakeQueue, magnitudeHeap, currentTime);

            if (event instanceof WatcherEvent) {
                processWatcherEvent((WatcherEvent) event, watcherTree, magnitudeHeap);
            } else if (event instanceof EarthquakeEvent) {
                processEarthquakeEvent((EarthquakeEvent) event, watcherTree, earthquakeQueue, magnitudeHeap, printAll);
            }
        }
    }

    public static void removeOldEarthquakes(LinkedList<EarthquakeEvent> earthquakeQueue, MaxHeap<EarthquakeEvent> magnitudeHeap, int currentTime) {
        while (!earthquakeQueue.isEmpty()) {
            EarthquakeEvent eq = earthquakeQueue.peek();
            if (currentTime - eq.time >= 6) {
                earthquakeQueue.poll();
                magnitudeHeap.remove(eq);
            } else {
                break;
            }
        }
    }

    public static void processWatcherEvent(WatcherEvent event, KDTree2D watcherTree, MaxHeap<EarthquakeEvent> magnitudeHeap) {
        if (event.action.equals("add")) {
            Point2D point = new Point2D(event.longitude, event.latitude, event.name);
            watcherTree.insert(point);
            System.out.println(event.name + " is added to the watcher-tree");
            System.out.println(); // <-- Boş satır eklendi
        } else if (event.action.equals("delete")) {
            boolean removed = watcherTree.removeByName(event.name);
            if (removed) {
                System.out.println(event.name + " is removed from the watcher-tree");
                System.out.println(); // <-- Boş satır eklendi
            }
        } else if (event.action.equals("query-largest")) {
            if (magnitudeHeap.isEmpty()) {
                System.out.println("No records");
            } else {
                EarthquakeEvent largestEq = magnitudeHeap.peek();
                System.out.println("Largest earthquake in the past 6 hours:");
                System.out.println("Magnitude " + largestEq.magnitude + " at " + largestEq.place );
            }
            System.out.println(); // <-- Boş satır eklendi
        }
    }

    public static void processEarthquakeEvent(EarthquakeEvent event, KDTree2D watcherTree, LinkedList<EarthquakeEvent> earthquakeQueue, MaxHeap<EarthquakeEvent> magnitudeHeap, boolean printAll) {
        earthquakeQueue.add(event);
        magnitudeHeap.insert(event);

        if (printAll) {
            System.out.println("Earthquake " + event.place + " is inserted into the earthquake-queue");
            System.out.println(); // <-- Boş satır eklendi
        }

        double notificationDistance = 2 * Math.pow(event.magnitude, 3);
        List<Point2D> nearbyWatchers = (List<Point2D>) watcherTree.rangeQueryCircular(event.longitude, event.latitude, notificationDistance);

        for (int i = 0; i < nearbyWatchers.size(); i++) {
            Point2D watcher = nearbyWatchers.get(i);
            double distance = Math.sqrt(Math.pow(watcher.getX() - event.longitude, 2) + Math.pow(watcher.getY() - event.latitude, 2));
            if (distance < notificationDistance) {
                System.out.println("Earthquake " + event.place + " is close to " + watcher.getName());
                System.out.println(); // <-- Boş satır eklendi
            }
        }
    }

}
