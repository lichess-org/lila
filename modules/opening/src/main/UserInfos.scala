package lila.opening

import reactivemongo.bson._
import reactivemongo.bson.Macros

import lila.db.Types.Coll
import lila.rating.Glicko
import lila.user.User

case class UserInfos(user: User, history: List[Attempt])

object UserInfos {

  private def historySize = 20

  import Attempt.attemptBSONHandler

  def apply(attemptColl: Coll) = new {

    def apply(user: User): Fu[UserInfos] = fetchAttempts(user.id) map { attempts =>
      new UserInfos(user, makeHistory(attempts))
    } recover {
      case e: Exception =>
        play.api.Logger("Puzzle UserInfos").error(e.getMessage)
        new UserInfos(user, Nil)
    }

    def apply(user: Option[User]): Fu[Option[UserInfos]] =
      user ?? { apply(_) map (_.some) }

    private def fetchAttempts(userId: String): Fu[List[Attempt]] =
      attemptColl.find(BSONDocument(
        Attempt.BSONFields.userId -> userId
      )).sort(BSONDocument(
        Attempt.BSONFields.date -> -1
      )).cursor[Attempt].collect[List](historySize)
  }

  private def makeHistory(attempts: List[Attempt]) = attempts.take(historySize)
}
