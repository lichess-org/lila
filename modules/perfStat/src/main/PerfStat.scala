package lila.perfStat

import chess.IntRating
import reactivemongo.api.bson.Macros.Annotations.Key
import java.time.Duration
import scalalib.HeapSort

import lila.rating.PerfType
import lila.rating.PerfType.GamePerf

extension (p: Pov) def loss = p.game.winner.map(_.color != p.color)

case class PerfStat(
    @Key("_id") id: String, // userId/perfId
    userId: UserId,
    perfType: PerfType,
    highest: Option[RatingAt],
    lowest: Option[RatingAt],
    bestWins: Results,
    worstLosses: Results,
    count: Count,
    resultStreak: ResultStreak,
    playStreak: PlayStreak
):
  def perfKey = perfType.key
  def agg(pov: Pov) =
    if !pov.game.finished then this
    else
      val thisYear = pov.game.createdAt.isAfter(nowInstant.minusYears(1))
      copy(
        highest = RatingAt.agg(highest, pov, 1),
        lowest = if thisYear then RatingAt.agg(lowest, pov, -1) else lowest,
        bestWins = if ~pov.win then bestWins.agg(pov, 1) else bestWins,
        worstLosses = if thisYear && ~pov.loss then worstLosses.agg(pov, -1) else worstLosses,
        count = count(pov),
        resultStreak = resultStreak.agg(pov),
        playStreak = playStreak.agg(pov)
      )
  def userIds = bestWins.userIds ::: worstLosses.userIds

object PerfStat:

  type Getter = (User, PerfType) => Fu[PerfStat]

  def makeId(userId: UserId, perf: GamePerf) = s"$userId/${perf.id}"

  def init(userId: UserId, perf: GamePerf) =
    PerfStat(
      id = makeId(userId, perf),
      userId = userId,
      perfType = perf,
      highest = none,
      lowest = none,
      bestWins = Results(Nil),
      worstLosses = Results(Nil),
      count = Count.init,
      resultStreak = ResultStreak(win = Streaks.init, loss = Streaks.init),
      playStreak = PlayStreak(nb = Streaks.init, time = Streaks.init, lastDate = none)
    )

case class ResultStreak(win: Streaks, loss: Streaks):
  def agg(pov: Pov) =
    copy(
      win = win.continueOrReset(~pov.win, pov)(1),
      loss = loss.continueOrReset(~pov.loss, pov)(1)
    )

case class PlayStreak(nb: Streaks, time: Streaks, lastDate: Option[Instant]):
  def agg(pov: Pov) =
    pov.game.durationSeconds.fold(this) { seconds =>
      val cont = seconds < 3 * 60 * 60 && isContinued(pov.game.createdAt)
      copy(
        nb = nb.continueOrStart(cont, pov)(1),
        time = time.continueOrStart(cont, pov)(seconds),
        lastDate = pov.game.movedAt.some
      )
    }
  def checkCurrent =
    if isContinued(nowInstant) then this
    else copy(nb = nb.reset, time = time.reset)
  private def isContinued(at: Instant) =
    lastDate.forall: ld =>
      at.isBefore(ld.plusMinutes(PlayStreak.expirationMinutes))
object PlayStreak:
  val expirationMinutes = 60

case class Streaks(cur: Streak, max: Streak):
  def continueOrReset(cont: Boolean, pov: Pov)(v: Int) =
    copy(cur = cur.continueOrReset(cont, pov)(v)).setMax
  def continueOrStart(cont: Boolean, pov: Pov)(v: Int) =
    copy(cur = cur.continueOrStart(cont, pov)(v)).setMax
  def reset          = copy(cur = Streak.init)
  private def setMax = copy(max = if cur.v >= max.v then cur else max)
object Streaks:
  val init = Streaks(Streak.init, Streak.init)
case class Streak(v: Int, from: Option[GameAt], to: Option[GameAt]):
  def continueOrReset(cont: Boolean, pov: Pov)(v: Int) =
    if cont then inc(pov, v) else Streak.init
  def continueOrStart(cont: Boolean, pov: Pov)(v: Int) =
    if cont then inc(pov, v)
    else
      val at  = GameAt(pov.game.createdAt, pov.gameId).some
      val end = GameAt(pov.game.movedAt, pov.gameId).some
      Streak(v, at, end)
  private def inc(pov: Pov, by: Int) =
    val at  = GameAt(pov.game.createdAt, pov.gameId).some
    val end = GameAt(pov.game.movedAt, pov.gameId).some
    Streak(v + by, from.orElse(at), end)
  def duration = Duration.ofSeconds(v)
object Streak:
  val init = Streak(0, none, none)

case class Count(
    all: Int,
    rated: Int,
    win: Int,
    loss: Int,
    draw: Int,
    tour: Int,
    berserk: Int,
    opAvg: Avg,
    seconds: Int,
    disconnects: Int
):
  def apply(pov: Pov) =
    copy(
      all = all + 1,
      rated = rated + (if pov.game.rated then 1 else 0),
      win = win + (if pov.win.contains(true) then 1 else 0),
      loss = loss + (if pov.win.contains(false) then 1 else 0),
      draw = draw + (if pov.win.isEmpty then 1 else 0),
      tour = tour + (if pov.game.isTournament then 1 else 0),
      berserk = berserk + (if pov.player.berserk then 1 else 0),
      opAvg = pov.opponent.stableRating.fold(opAvg)(r => opAvg.agg(r.value)),
      seconds = seconds + (pov.game.durationSeconds match
        case Some(s) if s <= 3 * 60 * 60 => s
        case _                           => 0),
      disconnects = disconnects + {
        if ~pov.loss && pov.game.status == chess.Status.Timeout then 1 else 0
      }
    )
  def duration = Duration.ofSeconds(seconds)
object Count:
  val init = Count(
    all = 0,
    rated = 0,
    win = 0,
    loss = 0,
    draw = 0,
    tour = 0,
    berserk = 0,
    opAvg = Avg(0, 0),
    seconds = 0,
    disconnects = 0
  )

case class Avg(avg: Double, pop: Int):
  def agg(v: Int) =
    copy(
      avg = ((avg * pop) + v) / (pop + 1),
      pop = pop + 1
    )

case class GameAt(at: Instant, gameId: GameId)
object GameAt:
  def agg(pov: Pov) = GameAt(pov.game.movedAt, pov.gameId)

case class RatingAt(int: IntRating, at: Instant, gameId: GameId)
object RatingAt:
  def agg(cur: Option[RatingAt], pov: Pov, comp: Int) =
    pov.player.stableRatingAfter
      .filter: r =>
        cur.forall: c =>
          r.value.compare(c.int.value) == comp
      .map:
        RatingAt(_, pov.game.movedAt, pov.gameId)
      .orElse(cur)

import reactivemongo.api.bson.Macros.Annotations.Key
case class Result(@Key("opInt") opRating: IntRating, opId: UserId, at: Instant, gameId: GameId)

case class Results(results: List[Result]):
  def agg(pov: Pov, comp: Int) = {
    for
      opId  <- pov.opponent.userId
      opInt <- pov.opponent.stableRating
      if pov.game.rated
      if pov.game.bothPlayersHaveMoved
    yield Results(
      HeapSort.topN(
        Result(
          opInt,
          opId,
          pov.game.movedAt,
          pov.gameId
        ) :: results,
        Results.nb
      )(using Ordering.by[Result, Int](_.opRating.value * comp))
    )
  } | this
  def userIds = results.map(_.opId)
object Results:
  val nb = 5
