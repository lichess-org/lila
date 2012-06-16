package lila
package core

import akka.event.Logging._
import akka.actor._

class AkkaLogger extends Actor with StdOutLogger {

  private val closedSocketMessage =  "Getting messages on a supposedly closed socket? frame: EOF"

  def receive = {
    case InitializeLogger(_) ⇒ sender ! LoggerInitialized
    case event: LogEvent ⇒
      if (!(event.message.toString contains closedSocketMessage))
        print(event)
  }
}
