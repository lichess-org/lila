package lila.hub

import scala.concurrent.duration._

import actorApi.map._
import akka.actor._
import akka.pattern.{ ask, pipe }
import makeTimeout.short
import scalaz.Monoid

trait ActorMap[A <: Actor] extends Actor {

  protected def mkActor(id: String): A

  protected def actorMapReceive: Receive = {

    case Get(id) ⇒ sender ! {
      (actors get id) | {
        context.actorOf(Props(mkActor(id)), name = id) ~ { actor ⇒
          actors = actors + (id -> actor)
          context watch actor
        }
      }
    }

    case Tell(id, msg) ⇒ withActor(id)(_ forward msg)

    case TellAll(msg)  ⇒ tellAll(msg)

    case Ask(id, msg)  ⇒ get(id) flatMap (_ ? msg) pipeTo sender

    case Size          ⇒ sender ! actors.size

    case Terminated(actor) ⇒ {
      context unwatch actor
      actors find (_._2 == actor) foreach {
        case (id, _) ⇒ actors = actors - id
      }
    }
  }

  protected def tellAll(msg: Any) {
    actors.values foreach (_ ! msg)
  }

  // sequential
  protected def askAll(msg: Any): Fu[List[Any]] = {
    actors.values.toList map (_ ? msg)
  } sequenceFu

  // concurrent
  protected def zipAll[A: Monoid: Manifest](msg: Any): Fu[A] = {
    actors.values.toList map (_ ? msg mapTo manifest[A])
  }.suml

  protected def get(id: String): Fu[ActorRef] = self ? Get(id) mapTo manifest[ActorRef]

  protected def withActor(id: String)(op: ActorRef ⇒ Unit) = get(id) foreach op

  private var actors = Map[String, ActorRef]()
}
