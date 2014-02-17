package lila.hub

import scala.concurrent.duration._

import actorApi.map._
import akka.actor._
import akka.pattern.{ ask, pipe }
import makeTimeout.short
import scalaz.Monoid

trait ActorMap[A <: Actor] extends Actor {

  private var actors = Map[String, ActorRef]()

  def mkActor(id: String): A

  def actorMapReceive: Receive = {

    case Get(id) => sender ! {
      (actors get id) | {
        context.actorOf(Props(mkActor(id)), name = id) ~ { actor =>
          actors = actors + (id -> actor)
          context watch actor
        }
      }
    }

    case Tell(id, msg) => withActor(id)(_ forward msg)

    case TellAll(msg)  => tellAll(msg)

    case Ask(id, msg)  => get(id) flatMap (_ ? msg) pipeTo sender

    case Size          => sender ! actors.size

    case Terminated(actor) => {
      context unwatch actor
      actors filter (_._2 == actor) foreach {
        case (id, _) => actors = actors - id
      }
    }
  }

  def tellAll(msg: Any) {
    actors.values foreach (_ ! msg)
  }

  // sequential
  def askAll(msg: Any): Fu[List[Any]] = {
    actors.values.toList map (_ ? msg)
  } sequenceFu

  // concurrent
  def zipAll[A: Monoid: Manifest](msg: Any): Fu[A] = {
    actors.values.toList map (_ ? msg mapTo manifest[A])
  }.suml

  def get(id: String): Fu[ActorRef] = self ? Get(id) mapTo manifest[ActorRef]

  def withActor(id: String)(op: ActorRef => Unit) = get(id) foreach op
}
