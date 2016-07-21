package lila.opening

import scala.util.{ Try, Success, Failure }

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONArray, BSONValue }
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
      attemptColl.exists($doc(
        Attempt.BSONFields.id -> Attempt.makeId(opening.id, user.id)
      ))

    def playedIds(user: User): Fu[BSONArray] =
      attemptColl.distinct[BSONValue, List](Attempt.BSONFields.openingId,
        $doc(Attempt.BSONFields.userId -> user.id).some
      ) map BSONArray.apply
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
