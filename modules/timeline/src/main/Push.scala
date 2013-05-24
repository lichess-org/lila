package lila.timeline

import akka.actor._
import akka.pattern.{ ask, pipe }
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import play.api.libs.json._
import play.api.templates.Html

import lila.db.api._
import lila.hub.actorApi.relation.GetFriends
import lila.hub.actorApi.timeline.propagation._
import lila.hub.actorApi.timeline.{ Propagate, Atom, ReloadTimeline }
import lila.hub.ActorLazyRef
import makeTimeout.short
import tube.entryTube

private[timeline] final class Push(
    lobbySocket: ActorLazyRef,
    renderer: ActorLazyRef,
    relationActor: ActorLazyRef) extends Actor {

  def receive = {

    case Propagate(data, propagations) ⇒ propagate(propagations) foreach { users ⇒
      if (users.nonEmpty) makeEntry(users, data) >>-
        (users foreach { u ⇒
          lobbySocket.ref ! ReloadTimeline(u)
        })
    }
  }

  private def propagate(propagations: List[Propagation]): Fu[List[String]] =
    (propagations map {
      case Users(ids)  ⇒ fuccess(ids)
      case Friends(id) ⇒ relationActor ? GetFriends(id) mapTo manifest[List[String]]
    }).sequence map (_.flatten.distinct)

  private def makeEntry(users: List[String], data: Atom): Fu[Entry] =
    Entry.make(users, data).fold(
      fufail[Entry]("[timeline] invalid entry data " + data)
    ) { entry ⇒
        $find(Json.obj("typ" -> entry.typ, "date" -> $gt($date(DateTime.now - 1.hour)))) flatMap { entries ⇒
          entries exists (_ similarTo entry) fold (
            fufail[Entry]("[timeline] a similar entry already exists"),
            $insert(entry) inject entry
          )
        }
      }
}
