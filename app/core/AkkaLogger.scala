package lila
package core

import akka.event.Logging._
import akka.actor._

class AkkaLogger extends Actor with StdOutLogger {

  def receive = {
    case InitializeLogger(_) ⇒ sender ! LoggerInitialized
    case event: LogEvent ⇒
      if (event.message.toString contains "Getting messages on a supposedly closed socket?")
        println(event.message)
      else
        print(event)
  }
}
