package lila.relay

import akka.actor._
import akka.pattern.pipe
import scala.concurrent.duration._

import lila.hub.actorApi.map.Tell

private[relay] final class FICStub extends Actor {

  def receive = {
    case command.ListTourney => sender ! Nil
    case command.ListGames   => sender ! Nil
  }
}
