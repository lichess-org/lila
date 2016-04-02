package lila.opening

import scala.util.{ Try, Success, Failure }

import org.joda.time.DateTime
import reactivemongo.bson.BSONArray
import reactivemongo.core.commands._

import lila.db.dsl._
import lila.user.{ User, UserRepo }

private[opening] final class OpeningApi(
    openingColl: Coll,
    attemptColl: Coll,
    nameColl: Coll,
    apiToken: String) {

  import Opening.openingBSONHandler

  object opening {

    def find(id: Opening.ID): Fu[Option[Opening]] =
      openingColl.byId[Opening](id)
  }

  object attempt {

    def find(openingId: Opening.ID, userId: String): Fu[Option[Attempt]] =
      attemptColl.find($doc(
        Attempt.BSONFields.id -> Attempt.makeId(openingId, userId)
      )).uno[Attempt]

    def add(a: Attempt) = attemptColl insert a void

    def hasPlayed(user: User, opening: Opening): Fu[Boolean] =
      attemptColl.count($doc(
        Attempt.BSONFields.id -> Attempt.makeId(opening.id, user.id)
      ).some) map (0!=)

    def playedIds(user: User, max: Int): Fu[BSONArray] = {
      val col = attemptColl
      import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Group, Limit, Match, Push }

      val playedIdsGroup =
        Group($boolean(true))("ids" -> Push(Attempt.BSONFields.openingId))

      col.aggregate(Match($doc(Attempt.BSONFields.userId -> user.id)),
        List(Limit(max), playedIdsGroup)).map(_.documents.headOption.flatMap(
          _.getAs[BSONArray]("ids")).getOrElse(BSONArray()))
    }
  }

  object identify {
    def apply(fen: String, max: Int): Fu[List[String]] = nameColl.find(
      $doc("_id" -> fen),
      $doc("_id" -> false)
    ).uno[Bdoc] map { obj =>
        ~obj.??(_.getAs[List[String]]("names"))
      }
  }
}
