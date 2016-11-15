package lila.setup

import chess.format.Forsyth
import chess.{ Game => ChessGame, Board, Situation, Clock, Speed }

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
  val variant: chess.variant.Variant

  // Creator player color
  val color: Color

  def hasClock = timeMode == TimeMode.RealTime

  lazy val creatorColor = color.resolve

  def makeGame(v: chess.variant.Variant): ChessGame =
    ChessGame(board = Board init v, clock = makeClock)

  def makeGame: ChessGame = makeGame(variant)

  def validClock = hasClock.fold(clockHasTime, true)

  def clockHasTime = time + increment > 0

  def makeClock = hasClock option justMakeClock

  protected def justMakeClock =
    Clock((time * 60).toInt, clockHasTime.fold(increment, 1))

  def makeDaysPerTurn: Option[Int] = (timeMode == TimeMode.Correspondence) option days
}

trait Positional { self: Config =>

  import chess.format.Forsyth, Forsyth.SituationPlus

  def fen: Option[String]

  def strictFen: Boolean

  lazy val validFen = variant != chess.variant.FromPosition || {
    fen ?? { f => ~(Forsyth <<< f).map(_.situation playable strictFen) }
  }

  def fenGame(builder: ChessGame => Game): Game = {
    val baseState = fen ifTrue (variant == chess.variant.FromPosition) flatMap Forsyth.<<<
    val (chessGame, state) = baseState.fold(makeGame -> none[SituationPlus]) {
      case sit@SituationPlus(Situation(board, color), _) =>
        val game = ChessGame(
          board = board,
          player = color,
          turns = sit.turns,
          startedAtTurn = sit.turns,
          clock = makeClock)
        if (Forsyth.>>(game) == Forsyth.initial) makeGame(chess.variant.Standard) -> none
        else game -> baseState
    }
    val game = builder(chessGame)
    state.fold(game) {
      case sit@SituationPlus(Situation(board, _), _) => game.copy(
        variant = chess.variant.FromPosition,
        castleLastMoveTime = game.castleLastMoveTime.copy(
          lastMove = board.history.lastMove.map(_.origDest),
          castles = board.history.castles
        ),
        turns = sit.turns)
    }
  }
}

object Config extends BaseConfig

trait BaseConfig {
  val variants = List(chess.variant.Standard.id, chess.variant.Chess960.id)
  val variantDefault = chess.variant.Standard

  val variantsWithFen = variants :+ chess.variant.FromPosition.id
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
    variantsWithVariants :+ chess.variant.FromPosition.id

  val speeds = Speed.all map (_.id)

  private val timeMin = 0
  private val timeMax = 180
  private val acceptableFractions = Set(1 / 2d, 3 / 4d, 3 / 2d)
  def validateTime(t: Double) =
    t >= timeMin && t <= timeMax && (t.isWhole || acceptableFractions(t))

  private val incrementMin = 0
  private val incrementMax = 180
  def validateIncrement(i: Int) = i >= incrementMin && i <= incrementMax
}
