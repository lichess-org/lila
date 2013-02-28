package lila.app
package core

import game.{ GameRepo, PgnRepo }
import round.{ Finisher, Meddler }
import bookmark.BookmarkApi

import com.mongodb.casbah.query.Imports._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import scalaz.effects._

final class Titivate(
    gameRepo: GameRepo,
    pgnRepo: PgnRepo,
    finisher: Finisher,
    meddler: Meddler,
    bookmarkApi: BookmarkApi) {

  val finishByClock: IO[Unit] = for {
    games ← gameRepo.candidatesToAutofinish
    _ ← putStrLn("[titivate] Finish %d games by clock" format games.size)
    _ ← (finisher outoftimes games).sequence
  } yield ()

  val finishAbandoned: IO[Unit] = for {
    games ← gameRepo abandoned 300
    _ ← putStrLn("[titivate] Finish %d abandoned games" format games.size)
    _ ← (games map meddler.finishAbandoned).sequence
  } yield ()

  val cleanupUnplayed: IO[Unit] = for {
    ids ← gameRepo.unplayedIds
    _ ← putStrLn("[titivate] Remove %d unplayed games" format ids.size)
    _ ← gameRepo removeIds ids
    _ ← bookmarkApi removeByGameIds ids
    _ ← pgnRepo removeIds ids
  } yield ()

  val cleanupNext: IO[Unit] = {

    val cursor = gameRepo.collection.find(
      ("next" $exists true) ++ ("ca" $gt (DateTime.now - 3.days)),
      DBObject("next" -> true)
    )
    val unsetNext = (id: String) ⇒ gameRepo.collection.update(
      DBObject("_id" -> id),
      $unset(Seq("next"))
    )

    io {
      for (game ← cursor) {
        game.getAs[String]("next") foreach { nextId ⇒
          if (!(gameRepo exists nextId).unsafePerformIO) {
            game.getAs[String]("_id") foreach unsetNext
          }
        }
      }
    }
  }
}
