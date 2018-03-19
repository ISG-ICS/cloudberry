package Tweet;

public class UserMention {
    private long id;
    private String id_str;
    private int[] indices;
    private String name;
    private String screen_name;
    //

    @Override
    public String toString() {
        return super.toString();
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setScreen_name(String screen_name) {
        this.screen_name = screen_name;
    }

    public String getScreen_name() {
        return screen_name;
    }

    public String getId_str() {
        return id_str;
    }

    public void setId_str(String id_str) {
        this.id_str = id_str;
    }

    public void setIndices(int[] indices) {
        this.indices = indices;
    }

    public long getId() {
        return id;
    }

    public int[] getIndices() {
        return indices;
    }
}
