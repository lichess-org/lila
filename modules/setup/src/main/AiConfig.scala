package lila.setup

import chess.format.FEN
import scala.concurrent.ExecutionContext

import lila.game.{ Game, IdGenerator, Player, Pov, Source }
import lila.lobby.Color
import lila.user.User

case class AiConfig(
    variant: chess.variant.Variant,
    timeMode: TimeMode,
    time: Double,
    increment: Int,
    days: Int,
    level: Int,
    color: Color,
    fen: Option[FEN] = None
) extends Config
    with Positional {

  val strictFen = true

  def >> = (variant.id, timeMode.id, time, increment, days, level, color.name, fen).some

  private def game(user: Option[User])(implicit idGenerator: IdGenerator): Fu[Game] =
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

  def pov(user: Option[User])(implicit idGenerator: IdGenerator) = game(user) dmap { Pov(_, creatorColor) }

  def timeControlFromPosition = variant != chess.variant.FromPosition || time >= 1
}

object AiConfig extends BaseConfig {

  def from(v: Int, tm: Int, t: Double, i: Int, d: Int, level: Int, c: String, fen: Option[FEN]) =
    new AiConfig(
      variant = chess.variant.Variant(v) err "Invalid game variant " + v,
      timeMode = TimeMode(tm) err s"Invalid time mode $tm",
      time = t,
      increment = i,
      days = d,
      level = level,
      color = Color(c) err "Invalid color " + c,
      fen = fen
    )

  val default = AiConfig(
    variant = variantDefault,
    timeMode = TimeMode.Unlimited,
    time = 5d,
    increment = 8,
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
        variant = chess.variant.Variant orDefault (r int "v"),
        timeMode = TimeMode orDefault (r int "tm"),
        time = r double "t",
        increment = r int "i",
        days = r int "d",
        level = r int "l",
        color = Color.White,
        fen = r.getO[FEN]("f").filter(_.value.nonEmpty)
      )

    def writes(w: BSON.Writer, o: AiConfig) =
      $doc(
        "v"  -> o.variant.id,
        "tm" -> o.timeMode.id,
        "t"  -> o.time,
        "i"  -> o.increment,
        "d"  -> o.days,
        "l"  -> o.level,
        "f"  -> o.fen
      )
  }
}
