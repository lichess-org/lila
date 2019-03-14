package lidraughts.puzzle

import reactivemongo.bson._

import draughts.variant.Variant
import lidraughts.db.dsl._
import lidraughts.user.User

case class UserInfos(user: User, history: List[Round.Mini])

object UserInfos {

  private def historySize = 15
  private def chartSize = 15

  import Round.RoundMiniBSONReader

  def apply(roundColl: Map[Variant, Coll]) = new {

    def apply(user: User, variant: Variant): Fu[UserInfos] = fetchRoundMinis(user.id, variant) map {
      new UserInfos(user, _)
    }

    def apply(user: Option[User], variant: Variant): Fu[Option[UserInfos]] =
      user ?? { u => apply(u, variant) map (_.some) }

    private def fetchRoundMinis(userId: String, variant: Variant): Fu[List[Round.Mini]] =
      roundColl(variant).find(
        $doc(Round.BSONFields.userId -> userId),
        $doc(
          "_id" -> false,
          Round.BSONFields.puzzleId -> true,
          Round.BSONFields.ratingDiff -> true,
          Round.BSONFields.rating -> true
        )
      ).sort($sort desc Round.BSONFields.date)
        .cursor[Round.Mini]()
        .gather[List](historySize atLeast chartSize)
        .map(_.reverse)
  }
}
