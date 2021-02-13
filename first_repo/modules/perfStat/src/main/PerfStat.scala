package lila.perfStat

import org.joda.time.{ DateTime, Period }

import lila.common.Heapsort
import lila.game.Pov
import lila.rating.PerfType

case class PerfStat(
    _id: String, // userId/perfId
    userId: UserId,
    perfType: PerfType,
    highest: Option[RatingAt],
    lowest: Option[RatingAt],
    bestWins: Results,
    worstLosses: Results,
    count: Count,
    resultStreak: ResultStreak,
    playStreak: PlayStreak
) {

  def id = _id

  def agg(pov: Pov) =
    if (!pov.game.finished) this
    else {
      val thisYear = pov.game.createdAt isAfter DateTime.now.minusYears(1)
      copy(
        highest = RatingAt.agg(highest, pov, 1),
        lowest = if (thisYear) RatingAt.agg(lowest, pov, -1) else lowest,
        bestWins = if (~pov.win) bestWins.agg(pov, 1) else bestWins,
        worstLosses = if (thisYear && ~pov.loss) worstLosses.agg(pov, -1) else worstLosses,
        count = count(pov),
        resultStreak = resultStreak agg pov,
        playStreak = playStreak agg pov
      )
    }

  def userIds = bestWins.userIds ::: worstLosses.userIds
}

object PerfStat {

  type Getter = (lila.user.User, PerfType) => Fu[PerfStat]

  def makeId(userId: String, perfType: PerfType) = s"$userId/${perfType.id}"

  def init(userId: String, perfType: PerfType) =
    PerfStat(
      _id = makeId(userId, perfType),
      userId = UserId(userId),
      perfType = perfType,
      highest = none,
      lowest = none,
      bestWins = Results(Nil),
      worstLosses = Results(Nil),
      count = Count.init,
      resultStreak = ResultStreak(win = Streaks.init, loss = Streaks.init),
      playStreak = PlayStreak(nb = Streaks.init, time = Streaks.init, lastDate = none)
    )
}

case class ResultStreak(win: Streaks, loss: Streaks) {
  def agg(pov: Pov) =
    copy(
      win = win(~pov.win, pov)(1),
      loss = loss(~pov.loss, pov)(1)
    )
}

case class PlayStreak(nb: Streaks, time: Streaks, lastDate: Option[DateTime]) {
  def agg(pov: Pov) =
    pov.game.durationSeconds.fold(this) { seconds =>
      val cont = seconds < 3 * 60 * 60 && isContinued(pov.game.createdAt)
      copy(
        nb = nb(cont, pov)(1),
        time = time(cont, pov)(seconds),
        lastDate = pov.game.movedAt.some
      )
    }
  def checkCurrent =
    if (isContinued(DateTime.now)) this
    else copy(nb = nb.reset, time = time.reset)
  private def isContinued(at: DateTime) =
    lastDate.fold(true) { ld =>
      at.isBefore(ld plusMinutes PlayStreak.expirationMinutes)
    }
}
object PlayStreak {
  val expirationMinutes = 60
}

case class Streaks(cur: Streak, max: Streak) {
  def apply(cont: Boolean, pov: Pov)(v: Int) =
    copy(
      cur = cur(cont, pov)(v)
    ).setMax
  def reset          = copy(cur = Streak.init)
  private def setMax = copy(max = if (cur.v >= max.v) cur else max)
}
object Streaks {
  val init = Streaks(Streak.init, Streak.init)
}
case class Streak(v: Int, from: Option[GameAt], to: Option[GameAt]) {
  def apply(cont: Boolean, pov: Pov)(v: Int) = if (cont) inc(pov, v) else Streak.init
  private def inc(pov: Pov, by: Int) =
    copy(
      v = v + by,
      from = from orElse GameAt(pov.game.createdAt, pov.gameId).some,
      to = GameAt(pov.game.movedAt, pov.gameId).some
    )
  def period = new Period(v * 1000L)
}
object Streak {
  val init = Streak(0, none, none)
}

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
) {
  def apply(pov: Pov) =
    copy(
      all = all + 1,
      rated = rated + (if (pov.game.rated) 1 else 0),
      win = win + (if (pov.win.contains(true)) 1 else 0),
      loss = loss + (if (pov.win.contains(false)) 1 else 0),
      draw = draw + (if (pov.win.isEmpty) 1 else 0),
      tour = tour + (if (pov.game.isTournament) 1 else 0),
      berserk = berserk + (if (pov.player.berserk) 1 else 0),
      opAvg = pov.opponent.stableRating.fold(opAvg)(opAvg.agg),
      seconds = seconds + (pov.game.durationSeconds match {
        case Some(s) if s <= 3 * 60 * 60 => s
        case _                           => 0
      }),
      disconnects = disconnects + {
        if (~pov.loss && pov.game.status == chess.Status.Timeout) 1 else 0
      }
    )
  def period = new Period(seconds * 1000L)
}
object Count {
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
}

case class Avg(avg: Double, pop: Int) {
  def agg(v: Int) =
    copy(
      avg = ((avg * pop) + v) / (pop + 1),
      pop = pop + 1
    )
}

case class GameAt(at: DateTime, gameId: String)
object GameAt {
  def agg(pov: Pov) = GameAt(pov.game.movedAt, pov.gameId)
}

case class RatingAt(int: Int, at: DateTime, gameId: String)
object RatingAt {
  def agg(cur: Option[RatingAt], pov: Pov, comp: Int) =
    pov.player.stableRatingAfter
      .filter { r =>
        cur.fold(true) { c =>
          r.compare(c.int) == comp
        }
      }
      .map {
        RatingAt(_, pov.game.movedAt, pov.gameId)
      } orElse cur
}

case class Result(opInt: Int, opId: UserId, at: DateTime, gameId: String)

case class Results(results: List[Result]) extends AnyVal {
  def agg(pov: Pov, comp: Int) =
    pov.opponent.stableRating
      .ifTrue(pov.game.rated)
      .ifTrue(pov.game.bothPlayersHaveMoved)
      .fold(this) { opInt =>
        Results(
          Heapsort.topN(
            Result(
              opInt,
              UserId(~pov.opponent.userId),
              pov.game.movedAt,
              pov.gameId
            ) :: results,
            Results.nb,
            Ordering.by[Result, Int](_.opInt * comp)
          )
        )
      }
  def userIds = results.map(_.opId)
}
object Results {
  val nb = 5
}

case class UserId(value: String) extends AnyVal
