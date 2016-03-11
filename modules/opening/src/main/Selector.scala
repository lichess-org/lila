package lila.opening

import scala.concurrent.duration._
import scala.util.Random

import reactivemongo.api.QueryOpts
import reactivemongo.bson.{ BSONDocument, BSONInteger, BSONArray }

import lila.db.Types.Coll
import lila.user.User

private[opening] final class Selector(
    openingColl: Coll,
    api: OpeningApi,
    toleranceStep: Int,
    toleranceMax: Int,
    modulo: Int) {

  val anonSkipMax = 1500

  def apply(me: Option[User]): Fu[Opening] = (me match {
    case None =>
      openingColl.find(BSONDocument())
        .options(QueryOpts(skipN = Random nextInt anonSkipMax))
        .one[Opening] flatten "Can't find a opening for anon player!"
    case Some(user) => api.attempt.playedIds(user, modulo) flatMap { ids =>
      tryRange(user, toleranceStep, ids)
    } recoverWith {
      case e: Exception => apply(none)
    }
  }).mon(_.opening.selector.time) >>- lila.mon.opening.selector.count()

  private def tryRange(user: User, tolerance: Int, ids: BSONArray): Fu[Opening] =
    openingColl.find(BSONDocument(
      Opening.BSONFields.id -> BSONDocument("$nin" -> ids),
      Opening.BSONFields.rating -> BSONDocument(
        "$gt" -> BSONInteger(user.perfs.opening.intRating - tolerance),
        "$lt" -> BSONInteger(user.perfs.opening.intRating + tolerance)
      )
    )).one[Opening] flatMap {
        case Some(opening) => fuccess(opening)
        case None => if ((tolerance + toleranceStep) <= toleranceMax)
          tryRange(user, tolerance + toleranceStep, ids)
        else fufail(s"Can't find a opening for user $user!")
      }
}
