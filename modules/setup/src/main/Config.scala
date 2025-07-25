package lila.setup

import chess.format.Fen
import chess.variant.{ FromPosition, Variant }
import chess.{ Clock, Game as ChessGame, Position, Speed }
import scalalib.model.Days

import lila.lobby.TriColor
import lila.rating.PerfType

private[setup] trait Config:

  // Whether or not to use a clock
  val timeMode: TimeMode

  // Clock time in minutes
  val time: Double

  // Clock increment in seconds
  val increment: Clock.IncrementSeconds

  // Correspondence days per turn
  val days: Days

  // Game variant code
  val variant: Variant

  def hasClock = timeMode == TimeMode.RealTime

  def makeGame(v: Variant): ChessGame =
    ChessGame(position = v.initialPosition, clock = makeClock.map(_.toClock))

  def makeGame: ChessGame = makeGame(variant)

  def validClock = !hasClock || clockHasTime

  def validSpeed(isBot: Boolean) =
    !isBot || makeClock.forall: c =>
      Speed(c) >= Speed.Bullet

  def clockHasTime = time + increment.value > 0

  def makeClock = hasClock.option(justMakeClock)

  protected def justMakeClock =
    Clock.Config(
      Clock.LimitSeconds((time * 60).toInt),
      if clockHasTime then increment else Clock.IncrementSeconds(1)
    )

  def makeDaysPerTurn: Option[Days] = (timeMode == TimeMode.Correspondence).option(days)

  def makeSpeed: Speed = chess.Speed(makeClock)

  def perfType: PerfType = lila.rating.PerfType(variant, makeSpeed)
  def perfKey = perfType.key

trait WithColor:
  self: Config =>

  // creator player color
  def color: TriColor

  lazy val creatorColor: Color = color.resolve()

trait Positional:
  self: Config =>

  def fen: Option[Fen.Full]

  def strictFen: Boolean

  lazy val validFen = variant != FromPosition ||
    fen.exists: f =>
      Fen.read(f).exists(_.playable(strictFen))

  def fenGame(builder: ChessGame => Fu[Game]): Fu[Game] =
    val baseState = fen
      .ifTrue(variant.fromPosition)
      .flatMap:
        Fen.readWithMoveNumber(FromPosition, _)
    val (chessGame, state) = baseState.fold(makeGame -> none):
      case sit @ Position.AndFullMoveNumber(s, _) =>
        val game = ChessGame(
          position = s,
          ply = sit.ply,
          startedAtPly = sit.ply,
          clock = makeClock.map(_.toClock)
        )
        if Fen.write(game).isInitial then makeGame(chess.variant.Standard) -> none
        else game -> baseState
    builder(chessGame).dmap { game =>
      state.fold(game) { case sit @ Position.AndFullMoveNumber(position, _) =>
        game.copy(
          chess = game.chess.copy(
            position = game.position.copy(
              history = position.history,
              variant = FromPosition
            ),
            ply = sit.ply
          )
        )
      }
    }

object Config extends BaseConfig

trait BaseConfig:
  val variants = List(chess.variant.Standard.id, chess.variant.Chess960.id)
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

  private val timeMin = 0
  private val timeMax = 180
  private val acceptableFractions = Set(1 / 4d, 1 / 2d, 3 / 4d, 3 / 2d)
  def validateTime(t: Double) =
    t >= timeMin && t <= timeMax && (t.isWhole || acceptableFractions(t))

  private val incrementMin = Clock.IncrementSeconds(0)
  private val incrementMax = Clock.IncrementSeconds(180)
  def validateIncrement(i: Clock.IncrementSeconds) = i >= incrementMin && i <= incrementMax
