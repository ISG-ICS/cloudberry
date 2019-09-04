package connection;

public class Response {

    private int option;
    private String timestamp;
    private String flag;
    private int edgesCnt;
    private int repliesCnt;
    private String data;
    private int isolatedEdgesCnt;

    public int getOption() {
        return option;
    }

    public void setOption(int option) {
        this.option = option;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public int getEdgesCnt() {
        return edgesCnt;
    }

    public void setEdgesCnt(int edgesCnt) {
        this.edgesCnt = edgesCnt;
    }

    public int getRepliesCnt() {
        return repliesCnt;
    }

    public void setRepliesCnt(int repliesCnt) {
        this.repliesCnt = repliesCnt;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getIsolatedEdgesCnt() {
        return isolatedEdgesCnt;
    }

    public void setIsolatedEdgesCnt(int isolatedEdgesCnt) {
        this.isolatedEdgesCnt = isolatedEdgesCnt;
    }
}
