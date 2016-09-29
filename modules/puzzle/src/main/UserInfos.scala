package lila.puzzle

import reactivemongo.bson._

import lila.db.dsl._
import lila.user.User

case class UserInfos(user: User, history: List[Round.Mini])

object UserInfos {

  private def historySize = 12
  private def chartSize = 12

  import Round.RoundMiniBSONReader

  def apply(roundColl: Coll) = new {

    def apply(user: User): Fu[UserInfos] = fetchRoundMinis(user.id) map {
      new UserInfos(user, _)
    }

    def apply(user: Option[User]): Fu[Option[UserInfos]] =
      user ?? { apply(_) map (_.some) }

    private def fetchRoundMinis(userId: String): Fu[List[Round.Mini]] =
      roundColl.find(
        $doc(Round.BSONFields.userId -> userId),
        $doc(
          "_id" -> false,
          Round.BSONFields.puzzleId -> true,
          Round.BSONFields.ratingDiff -> true
        )).sort($sort desc Round.BSONFields.date)
        .cursor[Round.Mini]()
        .gather[List](historySize atLeast chartSize)
  }
}
