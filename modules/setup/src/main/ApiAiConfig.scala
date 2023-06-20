package lila.setup

import chess.Clock
import chess.format.Fen
import chess.variant.{ FromPosition, Variant }

import lila.common.Days
import lila.game.{ Game, IdGenerator, Player, Pov, Source }
import lila.lobby.Color
import lila.user.User

final case class ApiAiConfig(
    variant: Variant,
    clock: Option[Clock.Config],
    daysO: Option[Days],
    color: Color,
    level: Int,
    fen: Option[Fen.Epd] = None
) extends Config
    with Positional:

  val strictFen = false

  val days      = daysO | Days(2)
  val time      = clock.so(_.limit.roundSeconds / 60)
  val increment = clock.fold(Clock.IncrementSeconds(0))(_.incrementSeconds)
  val timeMode =
    if (clock.isDefined) TimeMode.RealTime
    else if (daysO.isDefined) TimeMode.Correspondence
    else TimeMode.Unlimited

  private def game(user: Option[User])(using IdGenerator): Fu[Game] =
    fenGame { chessGame =>
      val perfPicker = lila.game.PerfPicker.mainOrDefault(
        chess.Speed(chessGame.clock.map(_.config)),
        chessGame.situation.board.variant,
        makeDaysPerTurn
      )
      Game
        .make(
          chess = chessGame,
          whitePlayer = creatorColor.fold(
            Player.make(chess.White, user, perfPicker),
            Player.make(chess.White, level.some)
          ),
          blackPlayer = creatorColor.fold(
            Player.make(chess.Black, level.some),
            Player.make(chess.Black, user, perfPicker)
          ),
          mode = chess.Mode.Casual,
          source = if (chessGame.board.variant.fromPosition) Source.Position else Source.Ai,
          daysPerTurn = makeDaysPerTurn,
          pgnImport = None
        )
        .withUniqueId
    }.dmap(_.start)

  def pov(user: Option[User])(using IdGenerator) = game(user) dmap { Pov(_, creatorColor) }

  def autoVariant =
    if (variant.standard && fen.exists(!_.isInitial)) copy(variant = FromPosition)
    else this

object ApiAiConfig extends BaseConfig:

  // lazy val clockLimitSeconds: Set[Int] = Set(0, 15, 30, 45, 60, 90) ++ (2 to 180).view.map(60 *).toSet

  def from(
      l: Int,
      v: Option[Variant.LilaKey],
      cl: Option[Clock.Config],
      d: Option[Days],
      c: Option[String],
      pos: Option[Fen.Epd]
  ) =
    new ApiAiConfig(
      variant = Variant.orDefault(v),
      clock = cl,
      daysO = d,
      color = Color.orDefault(~c),
      level = l,
      fen = pos
    ).autoVariant
