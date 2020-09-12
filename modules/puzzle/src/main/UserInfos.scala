package lila.puzzle

import reactivemongo.api.bson._

import lila.db.AsyncColl
import lila.db.dsl._
import lila.user.User

case class UserInfos(user: User, history: List[Round])

final class UserInfosApi(roundColl: AsyncColl, currentPuzzleId: User => Fu[Option[PuzzleId]])(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private val historySize = 15
  private val chartSize   = 15

  def apply(user: Option[User]): Fu[Option[UserInfos]] = user ?? { apply(_) dmap (_.some) }

  def apply(user: User): Fu[UserInfos] =
    for {
      current <- currentPuzzleId(user)
      rounds  <- fetchRounds(user.id, current)
    } yield UserInfos(user, rounds)

  private def fetchRounds(userId: User.ID, currentPuzzleId: Option[PuzzleId]): Fu[List[Round]] = {
    val idSelector = $doc("$regex" -> BSONRegex(s"^$userId:", "")) ++
      currentPuzzleId.?? { id =>
        $doc("$lte" -> s"$userId:${Round encode id}")
      }
    roundColl {
      _.find($doc(Round.BSONFields.id -> idSelector))
        .sort($sort desc Round.BSONFields.id)
        .cursor[Round]()
        .list(historySize atLeast chartSize)
        .dmap(_.reverse)
    }
  }
}
