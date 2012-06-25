package lila
package core

import game.GameRepo
import round.Finisher
import bookmark.BookmarkApi

import com.mongodb.casbah.query.Imports._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import scalaz.effects._

final class Titivate(
  gameRepo: GameRepo, 
  finisher: Finisher,
  bookmarkApi: BookmarkApi) {

  val finishByClock: IO[Unit] =
    for {
      games ← gameRepo.candidatesToAutofinish
      _ ← (finisher outoftimes games).sequence
    } yield ()

  val cleanupUnplayed = for {
    ids ← gameRepo.unplayedIds
    _ ← gameRepo removeIds ids
    _ ← bookmarkApi removeByGameIds ids
  } yield ()

  val cleanupNext: IO[Unit] = {

    val cursor = gameRepo.collection.find(
      ("next" $exists true) ++ ("createdAt" $gt (DateTime.now - 3.days)),
      DBObject("next" -> true)
    )
    val unsetNext = (id: String) ⇒ gameRepo.collection.update(
      DBObject("_id" -> id),
      $unset("next")
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
