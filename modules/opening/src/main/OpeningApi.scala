package lila.opening

import scala.util.{ Try, Success, Failure }

import org.joda.time.DateTime
import play.api.libs.json.JsValue
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONRegex, BSONArray, BSONBoolean }
import reactivemongo.core.commands._

import lila.db.Types.Coll
import lila.user.{ User, UserRepo }

private[opening] final class OpeningApi(
    openingColl: Coll,
    attemptColl: Coll,
    nameColl: Coll,
    apiToken: String) {

  import Opening.openingBSONHandler

  object opening {

    def find(id: Opening.ID): Fu[Option[Opening]] =
      openingColl.find(BSONDocument("_id" -> id)).one[Opening]

    def importOne(json: JsValue, token: String): Fu[Opening.ID] =
      if (token != apiToken) fufail("Invalid API token")
      else {
        import Generated.generatedJSONRead
        Try(json.as[Generated]) match {
          case Failure(err)       => fufail(err.getMessage)
          case Success(generated) => generated.toOpening.future flatMap insertOpening
        }
      }

    def insertOpening(opening: Opening.ID => Opening): Fu[Opening.ID] =
      lila.db.Util findNextId openingColl flatMap { id =>
        val o = opening(id)
        openingColl.count(BSONDocument("fen" -> o.fen).some) flatMap {
          case 0 => openingColl insert o inject o.id
          case _ => fufail("Duplicate opening")
        }
      }
  }

  object attempt {

    def find(openingId: Opening.ID, userId: String): Fu[Option[Attempt]] =
      attemptColl.find(BSONDocument(
        Attempt.BSONFields.id -> Attempt.makeId(openingId, userId)
      )).one[Attempt]

    def add(a: Attempt) = attemptColl insert a void

    def hasPlayed(user: User, opening: Opening): Fu[Boolean] =
      attemptColl.count(BSONDocument(
        Attempt.BSONFields.id -> Attempt.makeId(opening.id, user.id)
      ).some) map (0!=)

    def playedIds(user: User, max: Int): Fu[BSONArray] = {
      val col = attemptColl
      import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Group, Limit, Match, Push }

      val playedIdsGroup =
        Group(BSONBoolean(true))("ids" -> Push(Attempt.BSONFields.openingId))

      col.aggregate(Match(BSONDocument(Attempt.BSONFields.userId -> user.id)),
        List(Limit(max), playedIdsGroup)).map(_.documents.headOption.flatMap(
          _.getAs[BSONArray]("ids")).getOrElse(BSONArray()))
    }
  }

  object identify {
    def apply(fen: String, max: Int): Fu[List[String]] = nameColl.find(
      BSONDocument("_id" -> fen),
      BSONDocument("_id" -> false)
    ).one[BSONDocument] map { obj =>
        ~obj.??(_.getAs[List[String]]("names"))
      }
  }
}
