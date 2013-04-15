package lila.game

import tube.gameTube
import lila.db.api._
import lila.common.PimpedJson._

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

private[game] final class Titivate {

  // TODO move to round module
  // val finishByClock: Funit = for {
  //   games ← gameRepo.candidatesToAutofinish
  //   _ ← putStrLn("[titivate] Finish %d games by clock" format games.size)
  //   _ ← (finisher outoftimes games).sequence
  // } yield ()

  // val finishAbandoned: Funit = for {
  //   games ← gameRepo abandoned 300
  //   _ ← putStrLn("[titivate] Finish %d abandoned games" format games.size)
  //   _ ← (games map meddler.finishAbandoned).sequence
  // } yield ()

  def cleanupUnplayed: Funit = GameRepo.unplayedIds flatMap { ids ⇒
    fuloginfo("[titivate] Remove %d unplayed games" format ids.size) >>
      $remove[Game]($select byIds ids) >>
      // TODO use event dispatcher
      // bookmarkApi removeByGameIds ids >>
      (tube.pgnTube |> { implicit pgnT ⇒ $remove[JsObject]($select byIds ids) })
  }

  def cleanupNext: Funit = $query[Game](Json.obj(
    "next" -> $exists(true),
    "ca" -> $gt(DateTime.now - 3.days)
  )).projection(Json.obj("next" -> true))
    .cursor[JsObject]
    .enumerateBulks(50) run {
      Iteratee foreach { (games: Iterator[JsObject]) ⇒
        games.foreach { game ⇒
          for {
            id ← game str "_id"
            nextId ← game str "nextId"
          } {
            $count.exists[Game]($select(nextId)) foreach { exists ⇒
              $update[Game]($select(id), $unset("next")) doUnless exists
            }
          }
        }
      }
    }
}
