package lidraughts.setup

import draughts.{ DraughtsGame, Situation, Clock, Speed }
import draughts.variant.FromPosition
import draughts.format.FEN

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

  def validClock = !hasClock || clockHasTime

  def clockHasTime = time + increment > 0

  def makeClock = hasClock option justMakeClock

  protected def justMakeClock =
    Clock.Config((time * 60).toInt, if (clockHasTime) increment else 1)

  def makeDaysPerTurn: Option[Int] = (timeMode == TimeMode.Correspondence) option days
}

trait Positional { self: Config =>

  import draughts.format.Forsyth, Forsyth.SituationPlus

  def fen: Option[FEN]

  def strictFen: Boolean

  lazy val validFen = variant != FromPosition || {
    fen ?? { f => ~(Forsyth <<< f.value).map(_.situation playable strictFen) }
  }

  lazy val validKingCount = variant != FromPosition || {
    fen ?? { f => Forsyth.countKings(f.value) <= 30 }
  }

  def fenGame(builder: DraughtsGame => Game): Game = {
    val baseState = fen ifTrue (variant.fromPosition) flatMap { f =>
      Forsyth.<<<@(FromPosition, f.value)
    }
    val (draughtsGame, state) = baseState.fold(makeGame -> none[SituationPlus]) {
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
    val game = builder(draughtsGame)
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
    draughts.variant.Frysk.id :+
    draughts.variant.Antidraughts.id :+
    draughts.variant.Breakthrough.id :+
    draughts.variant.FromPosition.id
  val variantsWithVariants =
    variants :+
      draughts.variant.Frisian.id :+
      draughts.variant.Frysk.id :+
      draughts.variant.Antidraughts.id :+
      draughts.variant.Breakthrough.id :+
      draughts.variant.Russian.id
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
