package lila.setup

import chess.format.Fen
import chess.variant.{ FromPosition, Variant }
import chess.{ ByColor, Clock }
import scalalib.model.Days

import lila.core.game.{ IdGenerator, NewPlayer, Source }
import lila.core.user.GameUser
import lila.lobby.TriColor

final case class ApiAiConfig(
    variant: Variant,
    clock: Option[Clock.Config],
    daysO: Option[Days],
    color: TriColor,
    level: Int,
    fen: Option[Fen.Full] = None
) extends Config
    with Positional
    with WithColor:

  val strictFen = false

  val days = daysO | Days(2)
  val time = clock.so(_.limitInMinutes)
  val increment = clock.fold(Clock.IncrementSeconds(0))(_.incrementSeconds)
  val timeMode =
    if clock.isDefined then TimeMode.RealTime
    else if daysO.isDefined then TimeMode.Correspondence
    else TimeMode.Unlimited

  private def game(user: GameUser)(using idGenerator: IdGenerator, newPlayer: NewPlayer): Fu[Game] =
    fenGame: chessGame =>
      lila.rating.PerfType(chessGame.position.variant, chess.Speed(chessGame.clock.map(_.config)))
      idGenerator.withUniqueId:
        lila.core.game
          .newGame(
            chess = chessGame,
            players = ByColor: c =>
              if creatorColor == c
              then newPlayer(c, user)
              else newPlayer.anon(c, level.some),
            rated = chess.Rated.No,
            source = if chessGame.position.variant.fromPosition then Source.Position else Source.Ai,
            daysPerTurn = makeDaysPerTurn,
            pgnImport = None
          )
    .dmap(_.start)

  def pov(user: GameUser)(using IdGenerator, NewPlayer) = game(user).dmap { Pov(_, creatorColor) }

  def autoVariant =
    if variant.standard && fen.exists(!_.isInitial)
    then copy(variant = FromPosition)
    else this

object ApiAiConfig extends BaseConfig:

  // lazy val clockLimitSeconds: Set[Int] = Set(0, 15, 30, 45, 60, 90) ++ (2 to 180).view.map(60 *).toSet

  def from(
      l: Int,
      v: Option[Variant.LilaKey],
      cl: Option[Clock.Config],
      d: Option[Days],
      c: Option[String],
      pos: Option[Fen.Full]
  ) =
    ApiAiConfig(
      variant = Variant.orDefault(v),
      clock = cl,
      daysO = d,
      color = TriColor.orDefault(~c),
      level = l,
      fen = pos
    ).autoVariant
