package actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import controllers.GraphController;

/**
 * This class works as the actor of the WebSocket.
 * For each WebSocket connection, there will be one actor object.
 */
public class WebSocketActor extends AbstractActor {

    /**
     * For each actor, there will be one GraphController object.
     */
    private GraphController graphController = new GraphController();

    /**
     * Immutable and Serializable handler to an actor.
     */
    private final ActorRef out;

    /**
     * Construct WebSocket actor with handler.
     * @param out The corresponding handler.
     * @return the configuration object using in creating an actor.
     */
    public static Props props(ActorRef out) {
        return Props.create(WebSocketActor.class, () -> new WebSocketActor(out));
    }

    /**
     * Constructor for WebSocket actor.
     * @param out The corresponding handler.
     */
    private WebSocketActor(ActorRef out) {
        this.out = out;
    }

    /**
     * Handler method dealing with received requests.
     * @return the specialized "receive" behavior.
     */
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, message -> graphController.dispatcher(message, this))
                .build();
    }

    /**
     * Returns the response data by WebSocket.
     * @param s data string to be returned
     */
    public void returnData(String s) {
        out.tell(s, self());
    }

}
