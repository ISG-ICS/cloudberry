package Tweet;

public class Coordinates {
    private Float[] coordinates;
    private String type;

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return getType()+":"+getCoordinates().toString();
    }

    public String getType() {
        return type;
    }

    public void setCoordinates(Float[] coordinates) {
        this.coordinates = coordinates;
    }

    public Float[] getCoordinates() {
        return coordinates;
    }
}
