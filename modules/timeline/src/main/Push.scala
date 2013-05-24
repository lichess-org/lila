package lila.timeline

import akka.actor._
import akka.pattern.{ ask, pipe }
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import play.api.libs.json._
import play.api.templates.Html

import lila.db.api._
import lila.hub.actorApi.timeline._
import makeTimeout.short
import tube.entryTube

private[timeline] final class Push(
    lobbySocket: lila.hub.ActorLazyRef,
    renderer: lila.hub.ActorLazyRef) extends Actor {

  def receive = {
    case maker @ MakeEntry(user, typ, data) ⇒ makeEntry(user, typ, data) foreach { entry ⇒
      renderer ? entry map {
        case view: Html ⇒ EntryView(user, view.body)
      } pipeTo lobbySocket.ref
    } 
  }

  private def makeEntry(user: String, typ: String, data: JsValue): Fu[Entry] =
    Entry.make(user, typ, data).fold(
      fufail[Entry]("[timeline] invalid entry data " + data)
    ) { entry ⇒
        $find(Json.obj("user" -> user, "date" -> $gt($date(DateTime.now - 1.hour)))) flatMap { entries ⇒
          entries exists (_ similarTo entry) fold (
            fufail[Entry]("[timeline] a similar entry already exists"),
            $insert(entry) inject entry
          )
        }
      }
}
