package lila
package core

import game.GameRepo
import round.Finisher

import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import scalaz.effects._

final class Titivate(gameRepo: GameRepo, finisher: Finisher) {

  val finishByClock: IO[Unit] =
    for {
      games ← gameRepo.candidatesToAutofinish
      _ ← (finisher outoftimes games).sequence
    } yield ()

  val cleanupUnplayed = gameRepo.cleanupUnplayed

  val cleanupNext: IO[Unit] = {

    val cursor = gameRepo.collection.find(
      ("next" $exists true) ++ ("updatedAt" $gt (DateTime.now - 3.days)),
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
