package lila
package core

import akka.event.Logging._
import akka.actor._

class AkkaLogger extends Actor with StdOutLogger {

  private val closedSocketMessage = "Getting messages on a supposedly closed socket?"

  private var closedSocketCount = 0
  private val closedSocketBatch = 1000

  def receive = {
    case InitializeLogger(_) ⇒ sender ! LoggerInitialized
    case event: LogEvent ⇒
      if (event.message.toString contains closedSocketMessage) {
        closedSocketCount = closedSocketCount + 1
        if (closedSocketCount % closedSocketBatch == 0) {
          println("%sx %s".format(closedSocketBatch, event.message))
          closedSocketCount = 0
        }
      }
      else print(event)
  }
}
