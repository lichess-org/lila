package lila.opening

import reactivemongo.bson.BSONArray
import scala.concurrent.duration._
import scala.util.Random

import lila.db.dsl._
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
      openingColl.find($empty)
        .skip(Random nextInt anonSkipMax)
        .one[Opening] flatten "Can't find a opening for anon player!"
    case Some(user) => api.attempt.playedIds(user, modulo) flatMap { ids =>
      tryRange(user, toleranceStep, ids)
    } recoverWith {
      case e: Exception => apply(none)
    }
  }).mon(_.opening.selector.time) >>- lila.mon.opening.selector.count()

  private def tryRange(user: User, tolerance: Int, ids: BSONArray): Fu[Opening] =
    openingColl.one[Opening]($doc(
      Opening.BSONFields.id $nin ids,
      Opening.BSONFields.rating $gt
        (user.perfs.opening.intRating - tolerance) $lt
        (user.perfs.opening.intRating + tolerance)
    )) flatMap {
      case Some(opening) => fuccess(opening)
      case None => if ((tolerance + toleranceStep) <= toleranceMax)
        tryRange(user, tolerance + toleranceStep, ids)
      else fufail(s"Can't find a opening for user $user!")
    }
}
