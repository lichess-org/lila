package chess

import java.text.DecimalFormat

import Clock.Config

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

  def remainingTime(c: Color) = (players(c).remaining - pending(c)) nonNeg

  def outOfTime(c: Color, withGrace: Boolean) =
    players(c).remaining <=
      timerFor(c).fold(Centis(0)) { t =>
        if (withGrace) (toNow(t) - (players(c).lag.quota atMost Centis(200))) nonNeg
        else toNow(t)
      }

  def moretimeable(c: Color) = players(c).remaining.centis < 100 * 60 * 60 * 2

  def isInit = players.forall(_.isInit)

  def isRunning = timer.isDefined

  def start = if (isRunning) this else copy(timer = Some(now))

  def stop =
    timer.fold(this) { t =>
      copy(
        players = players.update(color, _.takeTime(toNow(t))),
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
      metrics: MoveMetrics = MoveMetrics(),
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
        val (lagComp, lagTrack) = player.lag.onMove(lag)

        val moveTime = (elapsed - lagComp) nonNeg

        val clockActive = gameActive && moveTime < player.remaining
        val inc         = clockActive ?? player.increment

        val newC = updatePlayer(color) {
          _.takeTime(moveTime - inc)
            .copy(lag = lagTrack)
        }

        if (clockActive) newC else newC.hardStop
      }
    }).switch

  // To do: safely add this to takeback to remove inc from player.
  // def deinc = updatePlayer(color, _.giveTime(-incrementOf(color)))

  def takeback = switch

  def giveTime(c: Color, t: Centis) =
    updatePlayer(c) {
      _.giveTime(t)
    }

  def setRemainingTime(c: Color, centis: Centis) =
    updatePlayer(c) {
      _.setRemaining(centis)
    }

  def incrementOf(c: Color) = players(c).increment

  def goBerserk(c: Color) = updatePlayer(c) { _.copy(berserk = true) }

  def berserked(c: Color) = players(c).berserk
  def lag(c: Color)       = players(c).lag

  def lagCompAvg = players map { ~_.lag.compAvg } reduce (_ avg _)

  // Lowball estimate of next move's lag comp for UI butter.
  def lagCompEstimate(c: Color) = players(c).lag.compEstimate

  def estimateTotalSeconds = config.estimateTotalSeconds
  def estimateTotalTime    = config.estimateTotalTime
  def increment            = config.increment
  def incrementSeconds     = config.incrementSeconds
  def limit                = config.limit
  def limitInMinutes       = config.limitInMinutes
  def limitSeconds         = config.limitSeconds
}

case class ClockPlayer(
    config: Clock.Config,
    lag: LagTracker,
    elapsed: Centis = Centis(0),
    berserk: Boolean = false
) {
  def limit = {
    if (berserk) config.initTime - config.berserkPenalty
    else config.initTime
  }

  def recordLag(l: Centis) = copy(lag = lag.recordLag(l))

  def isInit = elapsed.centis == 0

  def remaining = limit - elapsed

  def takeTime(t: Centis) = copy(elapsed = elapsed + t)

  def giveTime(t: Centis) = takeTime(-t)

  def setRemaining(t: Centis) = copy(elapsed = limit - t)

  def increment = if (berserk) Centis(0) else config.increment
}

object ClockPlayer {
  def withConfig(config: Clock.Config) =
    ClockPlayer(
      config,
      LagTracker.init(config)
    )
}

object Clock {
  private val limitFormatter = new DecimalFormat("#.##")

  // All unspecified durations are expressed in seconds
  case class Config(limitSeconds: Int, incrementSeconds: Int) {

    def berserkable = incrementSeconds == 0 || limitSeconds > 0

    def emergSeconds = math.min(60, math.max(10, limitSeconds / 8))

    def estimateTotalSeconds = limitSeconds + 40 * incrementSeconds

    def estimateTotalTime = Centis.ofSeconds(estimateTotalSeconds)

    def hasIncrement = incrementSeconds > 0

    def increment = Centis.ofSeconds(incrementSeconds)

    def limit = Centis.ofSeconds(limitSeconds)

    def limitInMinutes = limitSeconds / 60d

    def toClock = Clock(this)

    def limitString: String =
      limitSeconds match {
        case l if l % 60 == 0 => (l / 60).toString
        case 15 => "¼"
        case 30 => "½"
        case 45 => "¾"
        case 90 => "1.5"
        case _  => limitFormatter.format(limitSeconds / 60d)
      }

    def show = toString

    override def toString = s"$limitString+$incrementSeconds"

    def berserkPenalty =
      if (limitSeconds < 40 * incrementSeconds) Centis(0)
      else Centis(limitSeconds * (100 / 2))

    def initTime = {
      if (limitSeconds == 0) increment atLeast Centis(300)
      else limit
    }
  }

  // [TimeControl "600+2"] -> 10+2
  def readPgnConfig(str: String): Option[Config] =
    str.split('+') match {
      case Array(initStr, incStr) =>
        for {
          init <- parseIntOption(initStr)
          inc  <- parseIntOption(incStr)
        } yield Config(init, inc)
      case _ => none
    }

  def apply(limit: Int, increment: Int): Clock = apply(Config(limit, increment))

  def apply(config: Config): Clock = {
    val player = ClockPlayer.withConfig(config)
    Clock(
      config = config,
      color = White,
      players = Color.Map(player, player),
      timer = None
    )
  }
}
