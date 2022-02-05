package lila.setup

import shogi.{ Game => ShogiGame, Situation, Clock, Speed }
import shogi.variant.{ FromPosition, Variant }
import shogi.format.forsyth.Sfen

import lila.game.Game
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

  def makeGame(v: Variant): ShogiGame =
    ShogiGame(situation = Situation(v), clock = makeClock.map(_.toClock))

  def makeGame: ShogiGame = makeGame(variant)

  def validClock = !hasClock || clockHasTime

  def clockHasTime = time + increment + byoyomi > 0

  def makeClock = hasClock option justMakeClock

  protected def justMakeClock = Clock.Config(
    (time * 60).toInt,
    if (clockHasTime) increment else 0,
    if (clockHasTime) byoyomi else 5,
    periods
  )
  def makeDaysPerTurn: Option[Int] = (timeMode == TimeMode.Correspondence) option days
}

trait Positional { self: Config =>

  import shogi.format.forsyth.Sfen.SituationPlus

  def sfen: Option[Sfen]

  def strictSfen: Boolean

  lazy val validSfen = variant != FromPosition || {
    sfen exists { f =>
      (f.toSituationPlus(shogi.variant.Standard)).exists(_.situation.playable(strict = strictSfen, withImpasse = true))
    }
  }

  def sfenGame(builder: ShogiGame => Game): Game = {
    val baseState = sfen ifTrue (variant.fromPosition) flatMap {
      _.toSituationPlus(FromPosition)
    }
    val (shogiGame, state) = baseState.fold(makeGame -> none[SituationPlus]) {
      case sit @ SituationPlus(s, _) =>
        val game = ShogiGame(
          situation = s,
          plies = sit.plies,
          startedAtPly = sit.plies,
          startedAtMove = sit.moveNumber,
          clock = makeClock.map(_.toClock)
        )
        if (game.toSfen.initialOf(shogi.variant.Standard))
          makeGame(shogi.variant.Standard) -> none
        else game -> baseState
    }
    val game = builder(shogiGame)
    state.fold(game) {
      case parsed @ SituationPlus(sit, _) => {
        game.copy(
          shogi = game.shogi.copy(
            situation = game.situation.copy(
              board   = sit.board,
              hands   = sit.hands,
              history = sit.history,
              variant = FromPosition,
            ),
            plies = parsed.plies,
            startedAtMove = parsed.moveNumber
          )
        )
      }
    }
  }
}

object Config extends BaseConfig

trait BaseConfig {
  val variants       = List(shogi.variant.Standard.id)
  val variantDefault = shogi.variant.Standard

  val variantsWithSfen = variants :+ FromPosition.id
  val aiVariants = variants :+
    shogi.variant.Minishogi.id :+
    shogi.variant.FromPosition.id
  val variantsWithVariants =
    variants :+ shogi.variant.Minishogi.id
  val variantsWithSfenAndVariants =
    variantsWithVariants :+ FromPosition.id

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

  private val periodsMin      = 0 // todo 1
  private val periodsMax      = 5
  def validatePeriods(i: Int) = i >= periodsMin && i <= periodsMax
}
