package lila.setup

import shogi.{ Clock, Game => ShogiGame, Speed }
import shogi.variant.Variant
import shogi.format.forsyth.Sfen

import lila.lobby.Color

private[setup] trait Config {

  // Whether or not to use a clock
  val timeMode: TimeMode

  // Clock time in minutes
  val time: Double

  // Clock increment in seconds
  val increment: Int

  // Clock byoyomi in seconds
  val byoyomi: Int

  // Clock periods
  val periods: Int

  // Correspondence days per turn
  val days: Int

  // Game variant code
  val variant: Variant

  // Creator player color
  val color: Color

  def hasClock = timeMode == TimeMode.RealTime

  lazy val creatorColor = color.resolve

  def validClock = !hasClock || clockHasTime

  def clockHasTime = time + increment + byoyomi > 0

  def makeClock = hasClock option justMakeClock

  protected def justMakeClock = Clock.Config(
    (time * 60).toInt,
    if (clockHasTime) increment else 0,
    if (clockHasTime) byoyomi else 10,
    periods
  )
  def makeDaysPerTurn: Option[Int] = (timeMode == TimeMode.Correspondence) option days
}

trait Positional { self: Config =>

  def sfen: Option[Sfen]

  def strictSfen: Boolean

  lazy val validSfen =
    sfen.fold(true) { sf =>
      sf.toSituationPlus(variant).exists(_.situation.playable(strict = strictSfen, withImpasse = true))
    }

  def makeGame = ShogiGame(sfen, variant).withClock(makeClock.map(_.toClock))

}

object Config extends BaseConfig

trait BaseConfig {
  val variantDefault = shogi.variant.Standard
  val variants = List(
    shogi.variant.Standard.id,
    shogi.variant.Minishogi.id,
    shogi.variant.Chushogi.id,
    shogi.variant.Annanshogi.id,
    shogi.variant.Kyotoshogi.id,
    shogi.variant.Checkshogi.id
  )
  val aiVariants =
    List(shogi.variant.Standard.id, shogi.variant.Minishogi.id, shogi.variant.Kyotoshogi.id)

  val speeds = Speed.all.map(_.id)

  private val timeMin             = 0
  private val timeMax             = 180
  private val acceptableFractions = Set(1 / 4d, 1 / 2d, 3 / 4d, 3 / 2d)
  def validateTime(t: Double) =
    t >= timeMin && t <= timeMax && (t.isWhole || acceptableFractions(t))

  private val incrementMin      = 0
  private val incrementMax      = 180
  def validateIncrement(i: Int) = i >= incrementMin && i <= incrementMax

  private val byoyomiMin      = 0
  private val byoyomiMax      = 180
  def validateByoyomi(i: Int) = i >= byoyomiMin && i <= byoyomiMax

  private val periodsMin      = 0
  private val periodsMax      = 5
  def validatePeriods(i: Int) = i >= periodsMin && i <= periodsMax
}
