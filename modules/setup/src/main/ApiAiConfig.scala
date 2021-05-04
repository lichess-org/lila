package lila.setup

import shogi.Clock
import shogi.format.{ FEN, Forsyth }
import shogi.variant.{ FromPosition, Variant }
import lila.game.{ Game, Player, Pov, Source }
import lila.lobby.Color
import lila.user.User

final case class ApiAiConfig(
    variant: Variant,
    clock: Option[Clock.Config],
    daysO: Option[Int],
    color: Color,
    level: Int,
    fen: Option[FEN] = None
) extends Config
    with Positional {

  val strictFen = false

  def >> = (level, variant.key.some, clock, daysO, color.name.some, fen.map(_.value)).some

  val days      = ~daysO
  val increment = clock.??(_.increment.roundSeconds)
  val byoyomi   = clock.??(_.byoyomi.roundSeconds)
  val periods   = clock.??(_.periods)
  val time      = clock.??(_.limit.roundSeconds / 60)
  val timeMode =
    if (clock.isDefined) TimeMode.RealTime
    else if (daysO.isDefined) TimeMode.Correspondence
    else TimeMode.Unlimited

  def game(user: Option[User]) =
    fenGame { shogiGame =>
      val perfPicker = lila.game.PerfPicker.mainOrDefault(
        shogi.Speed(shogiGame.clock.map(_.config)),
        shogiGame.situation.board.variant,
        makeDaysPerTurn
      )
      Game
        .make(
          shogi = shogiGame,
          sentePlayer = creatorColor.fold(
            Player.make(shogi.Sente, user, perfPicker),
            Player.make(shogi.Sente, level.some)
          ),
          gotePlayer = creatorColor.fold(
            Player.make(shogi.Gote, level.some),
            Player.make(shogi.Gote, user, perfPicker)
          ),
          mode = shogi.Mode.Casual,
          source = if (shogiGame.board.variant.fromPosition) Source.Position else Source.Ai,
          daysPerTurn = makeDaysPerTurn,
          pgnImport = None
        )
        .sloppy
    } start

  def pov(user: Option[User]) = Pov(game(user), creatorColor)

  def autoVariant =
    if (variant.standard && fen.exists(_.value != Forsyth.initial)) copy(variant = FromPosition)
    else this
}

object ApiAiConfig extends BaseConfig {

  // lazy val clockLimitSeconds: Set[Int] = Set(0, 15, 30, 45, 60, 90) ++ (2 to 180).view.map(60 *).toSet

  def from(
      l: Int,
      v: Option[String],
      cl: Option[Clock.Config],
      d: Option[Int],
      c: Option[String],
      pos: Option[String]
  ) =
    new ApiAiConfig(
      variant = shogi.variant.Variant.orDefault(~v),
      clock = cl,
      daysO = d,
      color = Color.orDefault(~c),
      level = l,
      fen = pos map FEN
    ).autoVariant
}
