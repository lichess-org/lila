package lila.clas

import java.time.Duration
import reactivemongo.api.*
import reactivemongo.api.bson.*
import scalalib.model.Days
import chess.IntRating
import chess.rating.IntRatingDiff

import lila.core.game.GameRepo
import lila.core.user.WithPerf
import lila.db.dsl.{ *, given }
import lila.puzzle.PuzzleRound
import lila.rating.PerfType

case class ClasProgress(
    perfType: PerfType,
    days: Days,
    students: Map[UserId, StudentProgress]
):
  def apply(user: WithPerf) =
    students.getOrElse(
      user.id,
      StudentProgress(
        nb = 0,
        rating = (user.perf.intRating, user.perf.intRating),
        wins = 0,
        millis = 0
      )
    )

  def isPuzzle = perfType == PerfType.Puzzle

case class StudentProgress(
    nb: Int,
    wins: Int,
    millis: Long,
    rating: (IntRating, IntRating)
):
  def ratingProgress = (rating._2 - rating._1).into(IntRatingDiff)
  def winRate = if nb > 0 then wins * 100 / nb else 0
  def duration = Duration.ofMillis(millis)

final class ClasProgressApi(
    gameRepo: GameRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    historyApi: lila.core.history.HistoryApi,
    puzzleColls: lila.puzzle.PuzzleColls,
    filters: ClasUserFilters
)(using Executor):

  case class PlayStats(nb: Int, wins: Int, millis: Long)

  def apply(perfType: PerfType, days: Days, students: List[Student.WithUser]): Fu[ClasProgress] =
    val users = students.map(_.user)
    val userIds = users.map(_.id)

    val playStatsFu =
      if perfType == PerfType.Puzzle
      then getPuzzleStats(userIds, days)
      else getGameStats(perfType, userIds, days)

    val progressesFu = for
      usersWithPerf <- perfsRepo.withPerf(users, perfType)
      progresses <- historyApi.progresses(usersWithPerf, perfType.key, days)
    yield progresses

    playStatsFu.zip(progressesFu).map { (playStats, progresses) =>
      ClasProgress(
        perfType,
        days,
        users
          .zip(progresses)
          .map { (u, rating) =>
            val playStat = playStats.get(u.id)
            u.id -> StudentProgress(
              nb = playStat.so(_.nb),
              rating = rating,
              wins = playStat.so(_.wins),
              millis = playStat.so(_.millis)
            )
          }
          .toMap
      )
    }

  private def getPuzzleStats(userIds: List[UserId], days: Days): Fu[Map[UserId, PlayStats]] =
    puzzleColls.round:
      _.aggregateList(Int.MaxValue, _.sec): framework =>
        import framework.*
        Match(
          $doc(
            PuzzleRound.BSONFields.user.$in(userIds),
            PuzzleRound.BSONFields.date.$gt(nowInstant.minusDays(days.value))
          )
        ) -> List:
          GroupField("u")(
            "nb" -> SumAll,
            "win" -> Sum(
              $doc(
                "$cond" -> $arr("$w", 1, 0)
              )
            )
          )
      .map:
        _.flatMap: obj =>
          obj.getAsOpt[UserId]("_id").map { id =>
            id -> PlayStats(
              nb = ~obj.int("nb"),
              wins = ~obj.int("win"),
              millis = 0
            )
          }
        .toMap

  private def getGameStats(
      perfType: PerfType,
      userIds: List[UserId],
      days: Days
  ): Fu[Map[UserId, PlayStats]] =
    import lila.core.game.BSONFields as F
    gameRepo.coll
      .aggregateList(maxDocs = Int.MaxValue, _.sec): framework =>
        import framework.*
        Match(
          $doc(
            F.playerUids.$in(userIds),
            F.createdAt.$gte(nowInstant.minusDays(days.value)),
            gamePerfField -> perfType.id
          )
        ) -> List(
          Project(
            $doc(
              F.playerUids -> true,
              F.winnerId -> true,
              "ms" -> $doc("$subtract" -> $arr(s"$$${F.movedAt}", s"$$${F.createdAt}")),
              F.id -> false
            )
          ),
          UnwindField(F.playerUids),
          Match($doc(F.playerUids.$in(userIds))),
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
      .map:
        _.flatMap: obj =>
          obj.getAsOpt[UserId](F.id).map { id =>
            id -> PlayStats(
              nb = ~obj.int("nb"),
              wins = ~obj.int("win"),
              millis = ~obj.long("ms")
            )
          }
        .toMap

  private val gamePerfField = "pt"

  private[clas] def onFinishGame(game: Game): Unit =
    if game.userIds.exists(filters.student.is)
    then gameRepo.coll.updateFieldUnchecked($id(game.id), gamePerfField, game.perfKey.id)
