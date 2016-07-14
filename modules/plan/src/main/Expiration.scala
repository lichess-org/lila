package lila.plan

import lila.db.dsl._
import lila.user.{ User, UserRepo }

import org.joda.time.DateTime

private final class Expiration(patronColl: Coll) {

  import BsonHandlers._
  import PatronHandlers._

  def run: Funit = getExpired flatMap {
    _.map { patron =>
      patronColl.update($id(patron.id), patron.removeStripe.removePayPal) >>
        disableUserPlanOf(patron) >>-
        logger.info(s"Expired ${patron}")
    }.sequenceFu.void
  }

  private def disableUserPlanOf(patron: Patron): Funit =
    UserRepo byId patron.userId flatMap {
      _ ?? { user =>
        UserRepo.setPlan(user, user.plan.disable)
      }
    }

  private def getExpired =
    patronColl.list[Patron]($doc("expiresAt" $lt DateTime.now), 100)
}
