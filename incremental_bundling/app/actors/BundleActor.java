package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import controllers.GraphController;

public class BundleActor extends AbstractActor {

    private GraphController graphController = new GraphController();

    public static Props props(ActorRef out) {
        return Props.create(BundleActor.class, () -> new BundleActor(out));
    }

    private final ActorRef out;

    public BundleActor(ActorRef out) {
        this.out = out;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, message -> graphController.getData(message, this))
                .build();
    }

    public void returnData(String s) {
        out.tell(s, self());
    }

}
