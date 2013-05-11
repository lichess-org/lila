package lila.game

import tube.gameTube
import lila.db.api._
import lila.common.PimpedJson._
import lila.hub.actorApi.bookmark.Remove

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import akka.actor.ActorRef

private[game] final class Titivate(bookmark: ActorRef) {

  def cleanupUnplayed: Funit = GameRepo.unplayedIds flatMap { ids ⇒
    fuloginfo("[titivate] Remove %d unplayed games" format ids.size) >>
      $remove[Game]($select byIds ids) >>-
      (bookmark ! Remove(ids)) >>
      (tube.pgnTube |> { implicit pgnT ⇒ $remove[JsObject]($select byIds ids) })
  }

  def cleanupNext: Funit = $enumerate[JsObject](
    $query[Game](Json.obj(
      "next" -> $exists(true),
      "ca" -> $gt(DateTime.now - 3.days)
    )).projection(Json.obj("next" -> true))) { game =>
      ~(for {
        id ← game str "_id"
        nextId ← game str "nextId"
      } yield $count.exists[Game]($select(nextId)) flatMap { exists ⇒
        (!exists) ?? $update[Game]($select(id), $unset("next")) 
      })
    }
}
