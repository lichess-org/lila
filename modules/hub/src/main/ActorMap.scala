package lila.hub

import actorApi.map._
import akka.actor._

trait ActorMap extends Actor {

  private val actors = scala.collection.mutable.AnyRefMap.empty[String, ActorRef]

  def mkActor(id: String): Actor

  def actorMapReceive: Receive = {

    case Get(id) => sender ! getOrMake(id)

    case Tell(id, msg) => getOrMake(id) forward msg

    case TellAll(msg) => actors.foreachValue(_ forward msg)

    case TellIds(ids, msg) => ids foreach { id =>
      actors get id foreach (_ forward msg)
    }

    case Ask(id, msg) => getOrMake(id) forward msg

    case Terminated(actor) =>
      context unwatch actor
      actors foreach {
        case (id, a) => if (a == actor) actors -= id
      }

    case Exists(id) => sender ! actors.contains(id)
  }

  protected def size = actors.size

  private def getOrMake(id: String) = actors get id getOrElse {
    val actor = context.actorOf(Props(mkActor(id)), name = id)
    actors += (id -> actor)
    context watch actor
    actor
  }
}

object ActorMap {

  def apply(make: String => Actor) = new ActorMap {
    def mkActor(id: String) = make(id)
    def receive = actorMapReceive
  }
}
