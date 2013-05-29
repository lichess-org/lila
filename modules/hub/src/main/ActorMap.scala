package lila.hub

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi.map._
import makeTimeout.short

trait ActorMap[A <: Actor] extends Actor {

  def mkActor(id: String): A

  def actorMapReceive: Receive = {

    case Get(id) ⇒ sender ! {
      (actors get id) | {
        context.actorOf(Props(mkActor(id)), name = id) ~ { actor ⇒
          actors = actors + (id -> actor)
          context watch actor
        }
      }
    }

    case Tell(id, msg) ⇒ withActor(id)(_ forward msg)

    case Ask(id, msg)  ⇒ get(id) flatMap (_ ? msg) pipeTo sender

    case Size          ⇒ sender ! actors.size

    case Terminated(actor) ⇒ {
      context unwatch actor
      actors find (_._2 == actor) foreach {
        case (id, _) ⇒ actors = actors - id
      }
    }
  }

  def tellAll(msg: Any) {
    actors.values foreach (_ ! msg)
  }

  def askAll(msg: Any): Fu[List[Any]] = {
    actors.values.toList map (_ ? msg)
  } sequenceFu

  def get(id: String): Fu[ActorRef] = self ? Get(id) mapTo manifest[ActorRef]

  def withActor(id: String)(op: ActorRef ⇒ Unit) = get(id) foreach op

  private var actors = Map[String, ActorRef]()
}
