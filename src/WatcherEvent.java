public class WatcherEvent implements Event {
    public int time;
    public String action;
    public double longitude;
    public double latitude;
    public String name;

    public WatcherEvent(int time, String action, double longitude, double latitude, String name) {
        this.time = time;
        this.action = action;
        this.longitude = longitude;
        this.latitude = latitude;
        this.name = name;
    }

    public WatcherEvent(int time, String action, String name) {
        this.time = time;
        this.action = action;
        this.name = name;
    }

    public WatcherEvent(int time, String action) {
        this.time = time;
        this.action = action;
    }

    @Override
    public int getTime() {
        return this.time;
    }
}
