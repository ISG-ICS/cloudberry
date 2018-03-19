package Tweet;

public class BoundingBox {
    private Float[][][] coordinates;
    private String type;
    //

    @Override
    public String toString() {
        return getType()+":"+getCoordinates().toString();
    }

    public void setCoordinates(Float[][][] coordinates) {
        this.coordinates = coordinates;
    }

    public Float[][][] getCoordinates() {
        return coordinates;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
