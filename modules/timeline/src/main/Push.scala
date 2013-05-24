package lila.timeline

import lila.db.api.$insert
import lila.hub.actorApi.timeline._
import tube.entryTube
import makeTimeout.short

import play.api.templates.Html
import akka.actor._
import akka.pattern.{ ask, pipe }

private[timeline] final class Push(
    lobbySocket: lila.hub.ActorLazyRef,
    renderer: lila.hub.ActorLazyRef,
    getUsername: String ⇒ Fu[String]) extends Actor {

  def receive = {
    case maker @ MakeEntry(user, typ, data) ⇒ {
      Entry.make(user, typ, data).fold(fufail[Unit]("[timeline] invalid entry " + maker)) { entry ⇒
        $insert(entry) >>- {
          renderer ? entry map {
            case view: Html ⇒ EntryView(user, view.body)
          } pipeTo lobbySocket.ref
        }
      }
    } logFailure ("[timeline] push " + maker)
  }
}
