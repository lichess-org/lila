package lila.setup

import chess.{ Game => ChessGame, Situation, Clock, Speed }
import chess.variant.{ FromPosition, Variant }
import chess.format.FEN

import lila.game.Game
import lila.lobby.Color

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
  val variant: Variant

  // Creator player color
  val color: Color

  def hasClock = timeMode == TimeMode.RealTime

  lazy val creatorColor = color.resolve

  def makeGame(v: Variant): ChessGame =
    ChessGame(situation = Situation(v), clock = makeClock.map(_.toClock))

  def makeGame: ChessGame = makeGame(variant)

  def validClock = !hasClock || clockHasTime

  def validSpeed(isBot: Boolean) =
    !isBot || makeClock.fold(true) { c =>
      Speed(c) >= Speed.Bullet
    }

  def clockHasTime = time + increment > 0

  def makeClock = hasClock option justMakeClock

  protected def justMakeClock =
    Clock.Config((time * 60).toInt, if (clockHasTime) increment else 1)

  def makeDaysPerTurn: Option[Int] = (timeMode == TimeMode.Correspondence) option days
}

trait Positional { self: Config =>

  import chess.format.Forsyth, Forsyth.SituationPlus

  def fen: Option[FEN]

  def strictFen: Boolean

  lazy val validFen = variant != FromPosition || {
    fen exists { f =>
      (Forsyth <<< f.value).exists(_.situation playable strictFen)
    }
  }

  def fenGame(builder: ChessGame => Game): Game = {
    val baseState = fen ifTrue (variant.fromPosition) flatMap { f =>
      Forsyth.<<<@(FromPosition, f.value)
    }
    val (chessGame, state) = baseState.fold(makeGame -> none[SituationPlus]) {
      case sit @ SituationPlus(s, _) =>
        val game = ChessGame(
          situation = s,
          turns = sit.turns,
          startedAtTurn = sit.turns,
          clock = makeClock.map(_.toClock)
        )
        if (Forsyth.>>(game) == Forsyth.initial) makeGame(chess.variant.Standard) -> none
        else game                                                                 -> baseState
    }
    val game = builder(chessGame)
    state.fold(game) { case sit @ SituationPlus(Situation(board, _), _) =>
      game.copy(
        chess = game.chess.copy(
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
  val variants       = List(chess.variant.Standard.id, chess.variant.Chess960.id)
  val variantDefault = chess.variant.Standard

  val variantsWithFen = variants :+ FromPosition.id
  val aiVariants = variants :+
    chess.variant.Crazyhouse.id :+
    chess.variant.KingOfTheHill.id :+
    chess.variant.ThreeCheck.id :+
    chess.variant.Antichess.id :+
    chess.variant.Atomic.id :+
    chess.variant.Horde.id :+
    chess.variant.RacingKings.id :+
    chess.variant.FromPosition.id
  val variantsWithVariants =
    variants :+
      chess.variant.Crazyhouse.id :+
      chess.variant.KingOfTheHill.id :+
      chess.variant.ThreeCheck.id :+
      chess.variant.Antichess.id :+
      chess.variant.Atomic.id :+
      chess.variant.Horde.id :+
      chess.variant.RacingKings.id
  val variantsWithFenAndVariants =
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
}
