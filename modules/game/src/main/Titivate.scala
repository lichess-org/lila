package lila.game

import tube.gameTube
import lila.db.api._
import lila.common.PimpedJson._

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

private[game] final class Titivate {

  def cleanupUnplayed: Funit = GameRepo.unplayedIds flatMap { ids ⇒
    fuloginfo("[titivate] Remove %d unplayed games" format ids.size) >>
      $remove[Game]($select byIds ids) >>
      // TODO use event dispatcher
      // bookmarkApi removeByGameIds ids >>
      (tube.pgnTube |> { implicit pgnT ⇒ $remove[JsObject]($select byIds ids) })
  }

  def cleanupNext: Funit = $enumerate.bulk(
    $query[Game](Json.obj(
      "next" -> $exists(true),
      "ca" -> $gt(DateTime.now - 3.days)
    )).projection(Json.obj("next" -> true)).cursor[JsObject], 50) { (games: List[JsObject]) ⇒
      (games map { game ⇒
        ~(for {
          id ← game str "_id"
          nextId ← game str "nextId"
        } yield $count.exists[Game]($select(nextId)) flatMap { exists ⇒
          $update[Game]($select(id), $unset("next")) doUnless exists
        })
      }).sequence.void
    }
}
