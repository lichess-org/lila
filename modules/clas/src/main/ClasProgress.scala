package lila.clas

import lila.rating.PerfType
import lila.game.GameRepo
import lila.user.User
import lila.db.dsl._
import reactivemongo.api._
import reactivemongo.api.bson._

case class ClasProgress(
    perfType: PerfType,
    days: Int,
    students: Map[User.ID, StudentProgress]
) {
  def apply(user: User) =
    students.getOrElse(
      user.id,
      StudentProgress(
        nb = 0,
        rating = (user.perfs(perfType).intRating, user.perfs(perfType).intRating),
        opRating = 0,
        wins = 0
      )
    )
}

case class StudentProgress(
    nb: Int,
    rating: (Int, Int),
    opRating: Int,
    wins: Int
) {
  def ratingProgress = rating._2 - rating._1
  def winRate        = if (nb > 0) wins * 100 / nb else 0
}

final class ClasProgressApi(
    gameRepo: GameRepo,
    historyApi: lila.history.HistoryApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(perfType: PerfType, days: Int, students: List[Student.WithUser]): Fu[ClasProgress] = {
    val users = students.map(_.user)
    historyApi.progresses(users, perfType, days) map { progresses =>
      ClasProgress(
        perfType,
        days,
        users zip progresses map {
          case (u, rating) =>
            u.id -> StudentProgress(
              nb = 0,
              rating = rating,
              opRating = 0,
              wins = 0
            )
        } toMap
      )
    }
  }
}
