package lila.clas

import org.joda.time.{ DateTime, Period }
import reactivemongo.api._
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.game.{ Game, GameRepo }
import lila.puzzle.PuzzleRound
import lila.rating.PerfType
import lila.user.User

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

  def isPuzzle = perfType == PerfType.Puzzle
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
    puzzleColls: lila.puzzle.PuzzleColls,
    getStudentIds: () => Fu[Set[User.ID]]
)(implicit ec: scala.concurrent.ExecutionContext) {

  case class PlayStats(nb: Int, wins: Int, millis: Long)

  def apply(perfType: PerfType, days: Int, students: List[Student.WithUser]): Fu[ClasProgress] = {
    val users   = students.map(_.user)
    val userIds = users.map(_.id)

    val playStatsFu =
      if (perfType == PerfType.Puzzle) getPuzzleStats(userIds, days)
      else getGameStats(perfType, userIds, days)

    val progressesFu = historyApi.progresses(users, perfType, days)

    playStatsFu zip progressesFu map { case (playStats, progresses) =>
      ClasProgress(
        perfType,
        days,
        users zip progresses map { case (u, rating) =>
          val playStat = playStats get u.id
          u.id -> StudentProgress(
            nb = playStat.??(_.nb),
            rating = rating,
            wins = playStat.??(_.wins),
            millis = playStat.??(_.millis)
          )
        } toMap
      )
    }
  }

  private def getPuzzleStats(userIds: List[User.ID], days: Int): Fu[Map[User.ID, PlayStats]] =
    puzzleColls.round {
      _.aggregateList(
        maxDocs = Int.MaxValue,
        ReadPreference.secondaryPreferred
      ) { framework =>
        import framework._
        Match(
          $doc(
            PuzzleRound.BSONFields.user $in userIds,
            PuzzleRound.BSONFields.date $gt DateTime.now.minusDays(days)
          )
        ) -> List(
          GroupField("u")(
            "nb" -> SumAll,
            "win" -> Sum(
              $doc(
                "$cond" -> $arr($doc("$lt" -> $arr("$m", 0)), 1, 0)
              )
            )
          )
        )
      }.map {
        _.flatMap { obj =>
          obj.string("_id") map { id =>
            id -> PlayStats(
              nb = ~obj.int("nb"),
              wins = ~obj.int("win"),
              millis = 0
            )
          }
        }.toMap
      }
    }

  private def getGameStats(
      perfType: PerfType,
      userIds: List[User.ID],
      days: Int
  ): Fu[Map[User.ID, PlayStats]] = {
    import Game.{ BSONFields => F }
    import lila.game.Query
    gameRepo.coll
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
              "ms"         -> $doc("$subtract" -> $arr(s"$$${F.movedAt}", s"$$${F.createdAt}")),
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
            id -> PlayStats(
              nb = ~obj.int("nb"),
              wins = ~obj.int("win"),
              millis = ~obj.long("ms")
            )
          }
        }.toMap
      }
  }

  private[clas] def onFinishGame(game: lila.game.Game): Funit =
    game.userIds.nonEmpty ?? {
      getStudentIds() flatMap { studentIds =>
        game.userIds.exists(studentIds.contains) ?? gameRepo.denormalizePerfType(game)
      }
    }
}
