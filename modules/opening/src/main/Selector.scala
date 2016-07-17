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
    maxAttempts: Int) {

  val anonSkipMax = 1500

  def apply(me: Option[User]): Fu[Option[Opening]] = (me match {
    case None =>
      openingColl.find($empty)
        .skip(Random nextInt anonSkipMax)
        .uno[Opening]
    case Some(user) if user.perfs.opening.nb >= maxAttempts => fuccess(none)
    case Some(user) => api.attempt.playedIds(user) flatMap { ids =>
      tryRange(user, toleranceStep, ids)
    } recoverWith {
      case e: Exception => apply(none)
    }
  }).mon(_.opening.selector.time) >>- lila.mon.opening.selector.count()

  private def tryRange(user: User, tolerance: Int, ids: BSONArray): Fu[Option[Opening]] =
    openingColl.uno[Opening]($doc(
      Opening.BSONFields.id -> $doc("$nin" -> ids),
      Opening.BSONFields.rating $gt
        (user.perfs.opening.intRating - tolerance) $lt
        (user.perfs.opening.intRating + tolerance)
    )) flatMap {
      case None if (tolerance + toleranceStep) <= toleranceMax =>
        tryRange(user, tolerance + toleranceStep, ids)
      case res => fuccess(res)
    }
}
