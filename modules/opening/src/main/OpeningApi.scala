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
    apiToken: String) {

  import Opening.openingBSONHandler

  object opening {

    def find(id: Opening.ID): Fu[Option[Opening]] =
      openingColl.find(BSONDocument("_id" -> id)).one[Opening]

    def importOne(json: JsValue, token: String): Fu[Opening.ID] =
      if (token != apiToken) fufail("Invalid API token")
      else {
        import Generated.generatedJSONRead
        Try(json.as[Generated]).pp match {
          case Failure(err)       => fufail(err.getMessage)
          case Success(generated) => generated.toOpening.future flatMap insertOpening
        }
      }

    def insertOpening(opening: Opening.ID => Opening): Fu[Opening.ID] =
      lila.db.Util findNextId openingColl flatMap { id =>
        val o = opening(id)
        openingColl.db command Count(openingColl.name, BSONDocument("fen" -> o.fen).some) flatMap {
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
      attemptColl.db command Count(attemptColl.name, BSONDocument(
        Attempt.BSONFields.id -> Attempt.makeId(opening.id, user.id)
      ).some) map (0!=)

    private val PlayedIdsGroup = Group(BSONBoolean(true))("ids" -> Push(Attempt.BSONFields.openingId))

    def playedIds(user: User, max: Int): Fu[BSONArray] = {
      val command = Aggregate(attemptColl.name, Seq(
        Match(BSONDocument(Attempt.BSONFields.userId -> user.id)),
        Limit(max),
        PlayedIdsGroup
      ))
      attemptColl.db.command(command) map {
        _.headOption flatMap (_.getAs[BSONArray]("ids")) getOrElse BSONArray()
      }
    }
  }
}
