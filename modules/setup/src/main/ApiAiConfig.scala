package lila.setup

import shogi.Clock
import shogi.format.forsyth.Sfen
import shogi.variant.Variant
import lila.game.{ EngineConfig, Game, Player, Pov, Source }
import lila.lobby.Color
import lila.user.User

import scala.util.chaining._

final case class ApiAiConfig(
    variant: Variant,
    clock: Option[Clock.Config],
    daysO: Option[Int],
    color: Color,
    level: Int,
    sfen: Option[Sfen] = None
) extends Config
    with Positional {

  val strictSfen = false

  def >> = (level, variant.key.some, clock, daysO, color.name.some, sfen.map(_.value)).some

  val days      = ~daysO
  val increment = clock.??(_.increment.roundSeconds)
  val byoyomi   = clock.??(_.byoyomi.roundSeconds)
  val periods   = clock.??(_.periodsTotal)
  val time      = clock.??(_.limit.roundSeconds / 60)
  val timeMode =
    if (clock.isDefined) TimeMode.RealTime
    else if (daysO.isDefined) TimeMode.Correspondence
    else TimeMode.Unlimited

  def game(user: Option[User]) =
    makeGame pipe { shogiGame =>
      val perfPicker = lila.game.PerfPicker.mainOrDefault(
        shogi.Speed(shogiGame.clock.map(_.config)),
        shogiGame.variant,
        makeDaysPerTurn
      )
      Game
        .make(
          shogi = shogiGame,
          initialSfen = sfen,
          sentePlayer = creatorColor.fold(
            Player.make(shogi.Sente, user, perfPicker),
            Player.make(shogi.Sente, EngineConfig(sfen, shogiGame.variant, level).some)
          ),
          gotePlayer = creatorColor.fold(
            Player.make(shogi.Gote, EngineConfig(sfen, shogiGame.variant, level).some),
            Player.make(shogi.Gote, user, perfPicker)
          ),
          mode = shogi.Mode.Casual,
          source = if (sfen.filterNot(_.initialOf(variant)).isDefined) Source.Position else Source.Ai,
          daysPerTurn = makeDaysPerTurn,
          notationImport = None
        )
        .sloppy
    } start

  def pov(user: Option[User]) = Pov(game(user), creatorColor)

}

object ApiAiConfig extends BaseConfig {

  def from(
      l: Int,
      v: Option[String],
      cl: Option[Clock.Config],
      d: Option[Int],
      c: Option[String],
      sf: Option[String]
  ) =
    new ApiAiConfig(
      variant = shogi.variant.Variant.orDefault(~v),
      clock = cl.filter(c => c.limitSeconds > 0 || c.hasIncrement || c.hasByoyomi),
      daysO = d,
      color = Color.orDefault(~c),
      level = l,
      sfen = sf map Sfen.clean
    )
}
