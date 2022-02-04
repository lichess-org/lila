package shogi

import cats.syntax.option._
import java.text.DecimalFormat

import Clock.Config

case class CurrentClockInfo(time: Centis, periods: Int)

// All unspecified durations are expressed in seconds
case class Clock(
    config: Config,
    color: Color,
    players: Color.Map[ClockPlayer],
    timer: Option[Timestamp] = None,
    timestamper: Timestamper = RealTimestamper
) {
  import timestamper.{ now, toNow }

  @inline def timerFor(c: Color) = if (c == color) timer else None

  @inline def pending(c: Color) = timerFor(c).fold(Centis(0))(toNow)

  private def periodsInUse(c: Color, t: Centis): Int = {
    val player          = players(c)
    val remainingAfterT = player.remaining - t
    if (isRunning && !remainingAfterT.isPositive && player.byoyomi.isPositive)
      math.min((-remainingAfterT.centis / player.byoyomi.centis) + 1, player.periodsLeft)
    else 0
  }

  def currentClockFor(c: Color) = {
    val elapsed               = pending(c)
    val remainingAfterElapsed = players(c).remaining - elapsed
    val periods               = periodsInUse(c, elapsed)
    CurrentClockInfo(
      (remainingAfterElapsed + players(c).byoyomi * periods) nonNeg,
      periods + players(c).spentPeriods
    )
  }

  def outOfTime(c: Color, withGrace: Boolean) = {
    val player = players(c)
    player.remaining + player.periodsLeft * player.byoyomi <= timerFor(c).fold(Centis(0)) { t =>
      if (withGrace) (toNow(t) - (players(c).lag.quota atMost Centis(200))) nonNeg
      else toNow(t)
    }
  }

  def moretimeable(c: Color) = players(c).remaining.centis < 100 * 60 * 60 * 2

  def isRunning = timer.isDefined

  def start = if (isRunning) this else copy(timer = Option(now))

  def stop =
    timer.fold(this) { t =>
      val curT    = toNow(t)
      val periods = periodsInUse(color, curT)
      copy(
        players = players.update(
          color,
          _.takeTime(curT)
            .giveTime(byoyomiOf(color) * periods)
            .spendPeriods(periods)
            .copy(lastMoveTime = curT)
        ),
        timer = None
      )
    }

  def hardStop = copy(timer = None)

  def updatePlayer(c: Color)(f: ClockPlayer => ClockPlayer) =
    copy(players = players.update(c, f))

  def switch =
    copy(
      color = !color,
      timer = timer.map(_ => now)
    )

  def step(
      metrics: MoveMetrics = MoveMetrics.empty,
      gameActive: Boolean = true
  ) =
    (timer match {
      case None =>
        metrics.clientLag.fold(this) { l =>
          updatePlayer(color) { _.recordLag(l) }
        }
      case Some(t) => {
        val elapsed = toNow(t)
        val lag     = ~metrics.reportedLag(elapsed) nonNeg

        val player              = players(color)
        val remaining           = player.remaining
        val (lagComp, lagTrack) = player.lag.onMove(lag)
        val moveTime            = (elapsed - lagComp) nonNeg

        // As long as game is still in progress, and we have enough time left (including byoyomi and periods)
        val clockActive = gameActive && moveTime < remaining + player.periodsLeft * player.byoyomi
        // The number of periods the move stretched over
        val periodSpan = periodsInUse(color, moveTime)
        val usingByoyomi =
          clockActive && player.byoyomi.isPositive && (player.spentPeriods > 0 || periodSpan > 0)

        val newC =
          if (usingByoyomi)
            updatePlayer(color) {
              _.setRemaining((remaining - moveTime) atLeast player.byoyomi)
                .spendPeriods(periodSpan)
                .copy(lag = lagTrack, lastMoveTime = moveTime)
            }
          else
            updatePlayer(color) {
              _.takeTime(moveTime - (clockActive ?? player.increment))
                .spendPeriods(periodSpan)
                .copy(lag = lagTrack, lastMoveTime = moveTime)
            }

        if (clockActive) newC else newC.hardStop
      }
    }).switch

  def takeback = switch

  def refundPeriods(c: Color, p: Int) =
    updatePlayer(c) {
      _.refundPeriods(p)
    }

  def giveTime(c: Color, t: Centis) =
    updatePlayer(c) {
      _.giveTime(t)
    }

  def setRemainingTime(c: Color, t: Centis) =
    updatePlayer(c) {
      _.setRemaining(t)
    }

  def goBerserk(c: Color) = updatePlayer(c) { _.copy(berserk = true) }

  def incrementOf(c: Color)    = players(c).increment
  def byoyomiOf(c: Color)      = players(c).byoyomi
  def spentPeriodsOf(c: Color) = players(c).spentPeriods

  def lastMoveTimeOf(c: Color) = players(c).lastMoveTime

  def berserked(c: Color) = players(c).berserk
  def lag(c: Color)       = players(c).lag

  def lagCompAvg = players map { ~_.lag.compAvg } reduce (_ avg _)

  // Lowball estimate of next move's lag comp for UI butter.
  def lagCompEstimate(c: Color) = players(c).lag.compEstimate

  def estimateTotalSeconds = config.estimateTotalSeconds
  def estimateTotalTime    = config.estimateTotalTime
  def increment            = config.increment
  def incrementSeconds     = config.incrementSeconds
  def byoyomi              = config.byoyomi
  def byoyomiSeconds       = config.byoyomiSeconds
  def periodsTotal         = config.periodsTotal
  def limit                = config.limit
  def limitInMinutes       = config.limitInMinutes
  def limitSeconds         = config.limitSeconds
}

case class ClockPlayer(
    config: Clock.Config,
    lag: LagTracker,
    elapsed: Centis = Centis(0),
    spentPeriods: Int = 0,
    berserk: Boolean = false,
    lastMoveTime: Centis = Centis(0)
) {

  def limit =
    if (berserk) config.initTime - config.berserkPenalty
    else config.initTime

  def recordLag(l: Centis) = copy(lag = lag.recordLag(l))

  def periodsLeft = math.max((periodsTotal - spentPeriods), 0)

  def remaining = limit - elapsed

  def takeTime(t: Centis) = copy(elapsed = elapsed + t)

  def giveTime(t: Centis) = takeTime(-t)

  def setRemaining(t: Centis) = copy(elapsed = limit - t)

  def setPeriods(p: Int) = copy(spentPeriods = p)

  def spendPeriods(p: Int) = copy(spentPeriods = spentPeriods + p)

  def refundPeriods(p: Int) = spendPeriods(-(math.min(p, spentPeriods)))

  def increment = if (berserk) Centis(0) else config.increment

  def byoyomi = if (berserk) Centis(0) else config.byoyomi

  def periodsTotal = if (berserk) 0 else config.periodsTotal
}

object ClockPlayer {
  def withConfig(config: Clock.Config) =
    ClockPlayer(
      config,
      LagTracker.init(config)
    ).setPeriods(config.initPeriod)
}

object Clock {
  private val limitFormatter = new DecimalFormat("#.##")

  // All unspecified durations are expressed in seconds
  case class Config(limitSeconds: Int, incrementSeconds: Int, byoyomiSeconds: Int, periods: Int) {

    def berserkable = (incrementSeconds == 0 && byoyomiSeconds == 0) || limitSeconds > 0

    // Activate low time warning when between 10 and 90 seconds remain
    def emergSeconds = math.min(90, math.max(10, limitSeconds / 8))

    // Estimate 60 moves (per player) per game
    def estimateTotalSeconds = limitSeconds + 60 * incrementSeconds + 25 * periodsTotal * byoyomiSeconds

    def estimateTotalTime = Centis.ofSeconds(estimateTotalSeconds)

    def hasIncrement = incrementSeconds > 0

    def hasByoyomi = byoyomiSeconds > 0

    def increment = Centis.ofSeconds(incrementSeconds)

    def byoyomi = Centis.ofSeconds(byoyomiSeconds)

    def limit = Centis.ofSeconds(limitSeconds)

    def periodsTotal =
      if (hasByoyomi) math.max(periods, 1)
      else 0

    def limitInMinutes = limitSeconds / 60d

    def toClock = Clock(this)

    def startsAtZero = limitSeconds == 0 && hasByoyomi

    def berserkPenalty =
      if (limitSeconds < 60 * incrementSeconds || limitSeconds < 25 * byoyomiSeconds) Centis(0)
      else Centis(limitSeconds * (100 / 2))

    def initTime =
      if (limitSeconds == 0 && hasByoyomi) byoyomi atLeast Centis(500)
      else if (limitSeconds == 0) increment atLeast Centis(500)
      else limit

    def initPeriod = if (startsAtZero) 1 else 0

    def limitString: String =
      limitSeconds match {
        case l if l % 60 == 0 => (l / 60).toString
        case 15 => "¼"
        case 30 => "½"
        case 45 => "¾"
        case 90 => "1.5"
        case _  => limitFormatter.format(limitSeconds / 60d)
      }

    def incrementString: String = if (hasIncrement) s"+${incrementSeconds}" else ""

    def byoyomiString: String = if (hasByoyomi || !hasIncrement) s"|${byoyomiSeconds}" else ""

    def periodsString: String = if (periodsTotal > 1) s"(${periodsTotal}x)" else ""

    def show = toString

    override def toString = s"${limitString}${incrementString}${byoyomiString}${periodsString}"
  }

  def parseJPTime(str: String): Option[Int] = {
    if (str contains "時間")
      str
        .takeWhile(_ != '時')
        .toIntOption
        .map(_ * 3600 + (parseJPTime(str.reverse.takeWhile(_ != '間').reverse) | 0))
    else if (str contains "分")
      str
        .takeWhile(_ != '分')
        .toIntOption
        .map(_ * 60 + (parseJPTime(str.reverse.takeWhile(_ != '分').reverse) | 0))
    else str.filterNot(_ == '秒').toIntOption
  }

  val kifTime          = """(?:\d+(?:秒|分|時間)?)+"""
  lazy val KifClkRegex = raw"""($kifTime)(?:[\+|\|]($kifTime))?(?:\((\d)\))?(?:[\+|\|]($kifTime))?""".r

  // 持ち時間: 10分|20秒(1)+10 -> 600 init, 10inc, 20 byo, 1 per
  def readKifConfig(str: String): Option[Config] =
    str match {
      case KifClkRegex(initStr, byoStr, perStr, incStr) =>
        for {
          init <- parseJPTime(initStr)
          byo  <- Option(byoStr).fold(0.some)(parseJPTime _)
          per  <- Option(perStr).fold(1.some)(_ toIntOption)
          inc  <- Option(incStr).fold(0.some)(parseJPTime _)
        } yield Config(init, inc, byo, per)
      case _ => none
    }

  def readCsaConfig(str: String): Option[Config] =
    str.split("""\+|\|""") match {
      case Array(initStr, byoStr) =>
        for {
          init <- initStr.toIntOption
          byo  <- byoStr.toIntOption
        } yield Config(init, 0, byo, 1)
      case Array(initStr, byoStr, incStr) =>
        for {
          init <- initStr.toIntOption
          byo  <- byoStr.toIntOption
          inc  <- incStr.toIntOption
        } yield Config(init, inc, byo, 1)
      case _ => none
    }

  def apply(limit: Int, increment: Int, byoyomi: Int, periods: Int): Clock = {
    apply(Config(limit, increment, byoyomi, periods))
  }

  def apply(config: Config): Clock = {
    val player = ClockPlayer.withConfig(config)
    Clock(
      config = config,
      color = Sente,
      players = Color.Map(player, player),
      timer = None
    )
  }
}
