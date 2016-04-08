package lila.api

import akka.actor._

object DeadLetters {

  def start(system: ActorSystem) = {

    system.eventStream.subscribe(system.actorOf(Props(
      new Actor {
        def receive = {
          case d: DeadLetter => println(d)
        }
      })), classOf[DeadLetter])
  }
}
