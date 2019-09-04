package controllers;

import actors.WebSocketActor;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import play.libs.streams.ActorFlow;
import play.mvc.Controller;
import play.mvc.WebSocket;

import javax.inject.Inject;

/**
 * This class works as the controller of the WebSocket.
 */
public class WebSocketController extends Controller {

    private final ActorSystem actorSystem;
    private final Materializer materializer;

    @Inject
    public WebSocketController(ActorSystem actorSystem, Materializer materializer) {
        this.actorSystem = actorSystem;
        this.materializer = materializer;
    }

    /**
     * Forwards requests to the WebSocket actor.
     */
    public WebSocket socket() {
        return WebSocket.Text.accept(
                request -> ActorFlow.actorRef(WebSocketActor::props, actorSystem, materializer));
    }
}