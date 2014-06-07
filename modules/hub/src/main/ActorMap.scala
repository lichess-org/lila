package lila.hub

import scala.concurrent.duration._

import actorApi.map._
import akka.actor._
import akka.pattern.{ ask, pipe }
import makeTimeout.short
import scalaz.Monoid

trait ActorMap extends Actor {

  private var actors = Map[String, ActorRef]()

  def mkActor(id: String): Actor

  def actorMapReceive: Receive = {

    case Get(id)       => sender ! getOrMake(id)

    case Tell(id, msg) => getOrMake(id) forward msg

    case TellAll(msg)  => actors.values foreach (_ forward msg)

    case Ask(id, msg)  => getOrMake(id) forward msg

    case AskAll(msg)   => actors.values.map { _ ? msg }.sequenceFu pipeTo sender

    case Size          => sender ! actors.size

    case Terminated(actor) =>
      context unwatch actor
      actors filter (_._2 == actor) foreach {
        case (id, _) => actors = actors - id
      }
  }

  private def getOrMake(id: String) = (actors get id) | {
    context.actorOf(Props(mkActor(id)), name = id) ~ { actor =>
      actors = actors + (id -> actor)
      context watch actor
    }
  }
}

object ActorMap {

  def apply(make: String => Actor) = new ActorMap {
    def mkActor(id: String) = make(id)
    def receive = actorMapReceive
  }
}
