package lila
package command

import game.GameRepo
import scalaz.effects._

import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

final class GameCleanNext(gameRepo: GameRepo) {

  def apply(): IO[Unit] = io {

    val cursor = gameRepo.collection.find(
      DBObject("next" -> DBObject("$type" -> 3)) ++
        ("updatedAt" $gt (DateTime.now - 3.days)),
      DBObject("next" -> true)
    )
    val exists = (id: String) ⇒ gameRepo.collection.count(
      DBObject("_id" -> id)
    ) > 0
    val unsetNext = (id: String) ⇒ gameRepo.collection.update(
      DBObject("_id" -> id),
      $unset("next")
    )

    for (game ← cursor) {
      game.getAs[String]("next") foreach { nextId ⇒
        if (!exists(nextId)) {
          game.getAs[String]("_id") foreach unsetNext
        }
      }
    }
  }
}
