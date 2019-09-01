package models;

import java.util.Objects;

public class EdgeVector {
    public int getSourceNodeInd() {
        return sourceNodeInd;
    }

    public int getTargetNodeInd() {
        return targetNodeInd;
    }

    // TODO might merge this class with edge class
    private int sourceNodeInd;
    private int targetNodeInd;

    public EdgeVector(int sourceNodeInd, int targetNodeInd) {
        this.sourceNodeInd = sourceNodeInd;
        this.targetNodeInd = targetNodeInd;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof EdgeVector)) {
            return false;
        }
        EdgeVector edge = (EdgeVector) o;
        return sourceNodeInd == edge.sourceNodeInd && targetNodeInd == edge.targetNodeInd;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceNodeInd, targetNodeInd);
    }
}
