package lidraughts.setup

import draughts.{ DraughtsGame, Situation, Clock, Speed }
import draughts.variant.FromPosition

import lidraughts.game.Game
import lidraughts.lobby.Color

private[setup] trait Config {

  // Whether or not to use a clock
  val timeMode: TimeMode

  // Clock time in minutes
  val time: Double

  // Clock increment in seconds
  val increment: Int

  // Correspondence days per turn
  val days: Int

  // Game variant code
  val variant: draughts.variant.Variant

  // Creator player color
  val color: Color

  def hasClock = timeMode == TimeMode.RealTime

  lazy val creatorColor = color.resolve

  def makeGame(v: draughts.variant.Variant): DraughtsGame =
    DraughtsGame(situation = Situation(v), clock = makeClock.map(_.toClock))

  def makeGame: DraughtsGame = makeGame(variant)

  def validClock = hasClock.fold(clockHasTime, true)

  def clockHasTime = time + increment > 0

  def makeClock = hasClock option justMakeClock

  protected def justMakeClock =
    Clock.Config((time * 60).toInt, clockHasTime.fold(increment, 1))

  def makeDaysPerTurn: Option[Int] = (timeMode == TimeMode.Correspondence) option days
}

trait Positional { self: Config =>

  import draughts.format.Forsyth, Forsyth.SituationPlus

  def fen: Option[String]

  def strictFen: Boolean

  lazy val validFen = variant != FromPosition || {
    fen ?? { f => ~(Forsyth <<< f).map(_.situation playable strictFen) }
  }

  def fenGame(builder: DraughtsGame => Game): Game = {
    val baseState = fen ifTrue (variant.fromPosition) flatMap {
      Forsyth.<<<@(FromPosition, _)
    }
    val (chessGame, state) = baseState.fold(makeGame -> none[SituationPlus]) {
      case sit @ SituationPlus(s, _) =>
        val game = DraughtsGame(
          situation = s,
          turns = sit.turns,
          startedAtTurn = sit.turns,
          clock = makeClock.map(_.toClock)
        )
        if (Forsyth.>>(game) == Forsyth.initial) makeGame(draughts.variant.Standard) -> none
        else game -> baseState
    }
    val game = builder(chessGame)
    state.fold(game) {
      case sit @ SituationPlus(Situation(board, _), _) => game.copy(
        draughts = game.draughts.copy(
          situation = game.situation.copy(
            board = game.board.copy(
              history = board.history,
              variant = FromPosition
            )
          ),
          turns = sit.turns
        )
      )
    }
  }
}

object Config extends BaseConfig

trait BaseConfig {
  val variants = List(draughts.variant.Standard.id)
  val variantDefault = draughts.variant.Standard

  val variantsWithFen = variants :+ FromPosition.id
  val aiVariants = variants :+
    draughts.variant.Frisian.id :+
    draughts.variant.FromPosition.id
  val variantsWithVariants =
    variants :+
      draughts.variant.Frisian.id
  val variantsWithFenAndVariants =
    variantsWithVariants :+ FromPosition.id

  val speeds = Speed.all map (_.id)

  private val timeMin = 0
  private val timeMax = 180
  private val acceptableFractions = Set(1 / 4d, 1 / 2d, 3 / 4d, 3 / 2d)
  def validateTime(t: Double) =
    t >= timeMin && t <= timeMax && (t.isWhole || acceptableFractions(t))

  private val incrementMin = 0
  private val incrementMax = 180
  def validateIncrement(i: Int) = i >= incrementMin && i <= incrementMax
}
