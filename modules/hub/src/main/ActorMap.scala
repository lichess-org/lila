package lila.hub

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask

import actorApi.Tell
import makeTimeout.short

trait ActorMap[A <: Actor] extends Actor {

  def mkActor(id: String): A

  def receive = {

    case id: String ⇒ sender ! {
      (actors get id) | {
        context.actorOf(Props(mkActor(id)), name = id) ~ { actor ⇒
          actors = actors + (id -> actor)
          context watch actor
        }
      }
    }

    case Tell(id, msg) ⇒ get(id) foreach { _ forward msg }

    case Terminated(actor) ⇒ {
      context unwatch actor
      actors find (_._2 == actor) foreach {
        case (id, _) ⇒ actors = actors - id
      }
    }
  }

  private def get(id: String): Fu[ActorRef] = self ? id mapTo manifest[ActorRef]

  private var actors = Map[String, ActorRef]()
}
