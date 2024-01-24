package lila.setup

import shogi.format.forsyth.Sfen
import lila.game.{ EngineConfig, Game, Player, Pov, Source }
import lila.lobby.Color
import lila.user.User

import scala.util.chaining._

case class AiConfig(
    variant: shogi.variant.Variant,
    timeMode: TimeMode,
    time: Double,
    increment: Int,
    byoyomi: Int,
    periods: Int,
    days: Int,
    level: Int,
    color: Color,
    sfen: Option[Sfen] = None
) extends Config
    with Positional {

  val strictSfen = true

  def >> = (
    variant.id,
    timeMode.id,
    time,
    increment,
    byoyomi,
    periods,
    days,
    level,
    color.name,
    sfen.map(_.value)
  ).some

  def game(user: Option[User]) = {
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
  }

  def pov(user: Option[User]) = Pov(game(user), creatorColor)

  def timeControlNonStandard =
    variant.standard || time >= 1 || byoyomi >= 10 || increment >= 5
}

object AiConfig extends BaseConfig {

  def from(
      v: Int,
      tm: Int,
      t: Double,
      i: Int,
      b: Int,
      p: Int,
      d: Int,
      level: Int,
      c: String,
      sfen: Option[String]
  ) =
    new AiConfig(
      variant = shogi.variant.Variant(v) err "Invalid game variant " + v,
      timeMode = TimeMode(tm) err s"Invalid time mode $tm",
      time = t,
      increment = i,
      byoyomi = b,
      periods = p,
      days = d,
      level = level,
      color = Color(c) err "Invalid color " + c,
      sfen = sfen map Sfen.apply
    )

  val default = AiConfig(
    variant = variantDefault,
    timeMode = TimeMode.Unlimited,
    time = 5d,
    increment = 0,
    byoyomi = 10,
    periods = 1,
    days = 2,
    level = 1,
    color = Color.default
  )

  val levels = (1 to 8).toList

  val levelChoices = levels map { l =>
    (l.toString, l.toString, none)
  }

  import lila.db.BSON
  import lila.db.dsl._

  implicit private[setup] val aiConfigBSONHandler = new BSON[AiConfig] {

    def reads(r: BSON.Reader): AiConfig =
      AiConfig(
        variant = shogi.variant.Variant orDefault (r int "v"),
        timeMode = TimeMode orDefault (r int "tm"),
        time = r double "t",
        increment = r int "i",
        byoyomi = r intD "b",
        periods = r intD "p",
        days = r int "d",
        level = r int "l",
        color = Color.Sente,
        sfen = r.getO[Sfen]("f").filter(_.value.nonEmpty)
      )

    def writes(w: BSON.Writer, o: AiConfig) =
      $doc(
        "v"  -> o.variant.id,
        "tm" -> o.timeMode.id,
        "t"  -> o.time,
        "i"  -> o.increment,
        "b"  -> o.byoyomi,
        "p"  -> o.periods,
        "d"  -> o.days,
        "l"  -> o.level,
        "f"  -> o.sfen
      )
  }
}
