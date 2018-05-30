package lila.puzzle

import reactivemongo.bson._

import lila.db.dsl._
import lila.user.User

case class UserInfos(user: User, history: List[Round])

final class UserInfosApi(roundColl: Coll, currentPuzzleId: User => Fu[Option[PuzzleId]]) {

  private val historySize = 15
  private val chartSize = 15

  def apply(user: Option[User]): Fu[Option[UserInfos]] = user ?? { apply(_) map (_.some) }

  def apply(user: User): Fu[UserInfos] = for {
    current <- currentPuzzleId(user)
    rounds <- fetchRounds(user.id, current)
  } yield new UserInfos(user, rounds)

  private def fetchRounds(userId: User.ID, currentPuzzleId: Option[PuzzleId]): Fu[List[Round]] = {
    val idSelector = $doc("$regex" -> BSONRegex(s"^$userId:", "")) ++
      currentPuzzleId.?? { id => $doc("$lte" -> s"$userId:${Round encode id}") }
    roundColl.find($doc(Round.BSONFields.id -> idSelector))
      .sort($sort desc Round.BSONFields.id)
      .list[Round](historySize atLeast chartSize)
      .map(_.reverse)
  }
}
