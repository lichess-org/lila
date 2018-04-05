package lila.puzzle

import reactivemongo.bson._

import lila.db.dsl._
import lila.user.User

case class UserInfos(user: User, history: List[Round])

object UserInfos {

  private def historySize = 15
  private def chartSize = 15

  def apply(roundColl: Coll) = new {

    def apply(user: User): Fu[UserInfos] = fetchRounds(user.id) map {
      new UserInfos(user, _)
    }

    def apply(user: Option[User]): Fu[Option[UserInfos]] =
      user ?? { apply(_) map (_.some) }

    private def fetchRounds(userId: User.ID): Fu[List[Round]] =
      roundColl.find($doc(Round.BSONFields.id $startsWith s"$userId:"))
        .sort($sort desc Round.BSONFields.id)
        .cursor[Round]()
        .gather[List](historySize atLeast chartSize)
        .map(_.reverse)
  }
}
