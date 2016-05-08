package edu.uci.ics.cloudberry.zion.actor

import com.fasterxml.jackson.databind.JsonNode
import play.mvc.WebSocket

/**
 * A stub class that looks like WebSocket.Out to the rest of the system, and
 * returns the actual results of the test to check against our expectations.
 */
class StubOut() extends WebSocket.Out[JsonNode]() {
  var actual: JsonNode = null

  def write(node: JsonNode) {
    actual = node
  }

  def close() {}
}

