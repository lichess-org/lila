package lila.opening

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONDouble }

import lila.db.Types.Coll
import lila.rating.{ Glicko, Perf }
import lila.user.{ User, UserRepo }

private[opening] final class Finisher(
    api: OpeningApi,
    openingColl: Coll) {

  private def computeScore(opening: Opening, found: Int, failed: Int): Int = {
    val base = 100d * found / goal

  }

  def apply(opening: Opening, user: User, found: Int, failed: Int): Fu[Attempt] =
    api.attempt.find(opening.id, user.id) flatMap {
      case Some(a) => fuccess(a)
      case None =>
        val date = DateTime.now
        val score = computeScore(opening, found, failed)
        val userScorePerf = user.perfs.opening.add(score, DateTime.now)
        val openingScore = 0.1 * (score - opening.score) + opening.score
        val a = new Attempt(
          id = Attempt.makeId(opening.id, user.id),
          openingId = opening.id,
          userId = user.id,
          date = DateTime.now,
          score = score)
        ((api.attempt add a) >> {
          openingColl.update(
            BSONDocument("_id" -> opening.id),
            BSONDocument("$inc" -> BSONDocument(
              Opening.BSONFields.attempts -> BSONInteger(1)
            )) ++ BSONDocument("$set" -> BSONDocument(
              Opening.BSONFields.score -> BSONDouble(openingScore)
            ))) zip UserRepo.setScorePerf(user.id, "opening", userScorePerf)
        }) recover {
          case e: reactivemongo.core.commands.LastError if e.getMessage.contains("duplicate key error") => ()
        } inject a
    }
}
