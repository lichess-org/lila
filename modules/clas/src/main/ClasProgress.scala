package lila.clas

import org.joda.time.{ DateTime, Period }

import lila.rating.PerfType
import lila.game.{ Game, GameRepo }
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
        wins = 0,
        millis = 0
      )
    )
}

case class StudentProgress(
    nb: Int,
    wins: Int,
    millis: Long,
    rating: (Int, Int)
) {
  def ratingProgress = rating._2 - rating._1
  def winRate        = if (nb > 0) wins * 100 / nb else 0
  def period         = new Period(millis)
}

final class ClasProgressApi(
    gameRepo: GameRepo,
    historyApi: lila.history.HistoryApi,
    getStudentIds: () => Fu[Set[User.ID]]
)(implicit ec: scala.concurrent.ExecutionContext) {

  case class GameStats(nb: Int, wins: Int, millis: Long)

  def apply(perfType: PerfType, days: Int, students: List[Student.WithUser]): Fu[ClasProgress] = {
    val users   = students.map(_.user)
    val userIds = users.map(_.id)
    import Game.{ BSONFields => F }
    import lila.game.Query

    val gameStatsFu = gameRepo.coll
      .aggregateList(
        maxDocs = Int.MaxValue,
        ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
        Match(
          $doc(
            F.playerUids $in userIds,
            Query.createdSince(DateTime.now minusDays days),
            F.perfType -> perfType.id
          )
        ) -> List(
          Project(
            $doc(
              F.playerUids -> true,
              F.winnerId   -> true,
              "ms"         -> $doc("$subtract" -> $arr("$ua", "$ca")),
              F.id         -> false
            )
          ),
          UnwindField(F.playerUids),
          Match($doc(F.playerUids $in userIds)),
          GroupField(F.playerUids)(
            "nb" -> SumAll,
            "win" -> Sum(
              $doc(
                "$cond" -> $arr($doc("$eq" -> $arr("$us", "$wid")), 1, 0)
              )
            ),
            "ms" -> SumField("ms")
          )
        )
      }
      .map {
        _.flatMap { obj =>
          obj.string(F.id) map { id =>
            id -> GameStats(
              nb = ~obj.int("nb"),
              wins = ~obj.int("win"),
              millis = ~obj.long("ms")
            )
          }
        }.toMap
      }

    val progressesFu = historyApi.progresses(users, perfType, days)

    gameStatsFu zip progressesFu map {
      case (gameStats, progresses) =>
        ClasProgress(
          perfType,
          days,
          users zip progresses map {
            case (u, rating) =>
              val gameStat = gameStats get u.id
              u.id -> StudentProgress(
                nb = gameStat.??(_.nb),
                rating = rating,
                wins = gameStat.??(_.wins),
                millis = gameStat.??(_.millis)
              )
          } toMap
        )
    }
  }

  private[clas] def onFinishGame(game: lila.game.Game): Funit =
    game.userIds.nonEmpty ?? {
      getStudentIds() flatMap { studentIds =>
        game.userIds.exists(studentIds.contains) ?? gameRepo.denormalizePerfType(game)
      }
    }
}
