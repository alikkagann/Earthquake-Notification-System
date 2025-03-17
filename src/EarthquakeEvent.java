import java.util.Objects;

public class EarthquakeEvent implements Event {
    public int time;
    public String id;
    public String place;
    public double latitude;
    public double longitude;
    public double depth;
    public double magnitude;

    public EarthquakeEvent() {}

    public EarthquakeEvent(int time, String id, String place, double longitude, double latitude, double depth, double magnitude) {
        this.time = time;
        this.id = id;
        this.place = place;
        this.latitude = latitude;
        this.longitude = longitude;
        this.depth = depth;
        this.magnitude = magnitude;
    }

    @Override
    public int getTime() {
        return this.time;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof EarthquakeEvent))
            return false;
        EarthquakeEvent other = (EarthquakeEvent) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
