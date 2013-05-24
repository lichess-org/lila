package lila.timeline

import akka.actor._
import akka.pattern.{ ask, pipe }
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import play.api.libs.json._
import play.api.templates.Html

import lila.db.api._
import lila.hub.actorApi.timeline.{ MakeEntry, Atom, ReloadTimeline }
import makeTimeout.short
import tube.entryTube

private[timeline] final class Push(
    lobbySocket: lila.hub.ActorLazyRef,
    renderer: lila.hub.ActorLazyRef) extends Actor {

  def receive = {
    case MakeEntry(users, data) ⇒ makeEntry(users, data) >>-
      (users foreach { u ⇒
        lobbySocket.ref ! ReloadTimeline(u)
      })
  }

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
