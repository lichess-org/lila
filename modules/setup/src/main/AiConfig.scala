package lidraughts.setup

import lidraughts.game.{ Game, Player, Source, Pov }
import lidraughts.lobby.Color
import lidraughts.user.User

case class AiConfig(
    variant: draughts.variant.Variant,
    timeMode: TimeMode,
    time: Double,
    increment: Int,
    days: Int,
    level: Int,
    color: Color,
    fen: Option[String] = None
) extends Config with Positional {

  val strictFen = true

  def >> = (variant.id, timeMode.id, time, increment, days, level, color.name, fen).some

  def game(user: Option[User]) = fenGame { draughtsGame =>
    val perfPicker = lidraughts.game.PerfPicker.mainOrDefault(
      draughts.Speed(draughtsGame.clock.map(_.config)),
      draughtsGame.situation.board.variant,
      makeDaysPerTurn
    )
    Game.make(
      draughts = draughtsGame,
      whitePlayer = creatorColor.fold(
        Player.make(draughts.White, user, perfPicker),
        Player.make(draughts.White, level.some)
      ),
      blackPlayer = creatorColor.fold(
        Player.make(draughts.Black, level.some),
        Player.make(draughts.Black, user, perfPicker)
      ),
      mode = draughts.Mode.Casual,
      source = (draughtsGame.board.variant.fromPosition).fold(Source.Position, Source.Ai),
      daysPerTurn = makeDaysPerTurn,
      pdnImport = None
    )
  } start

  def pov(user: Option[User]) = Pov(game(user), creatorColor)
}

object AiConfig extends BaseConfig {

  def <<(v: Int, tm: Int, t: Double, i: Int, d: Int, level: Int, c: String, fen: Option[String]) = new AiConfig(
    variant = draughts.variant.Variant(v) err "Invalid game variant " + v,
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

  val levelChoices = levels map { l => (l.toString, l.toString, none) }

  import lidraughts.db.BSON
  import lidraughts.db.dsl._

  private[setup] implicit val aiConfigBSONHandler = new BSON[AiConfig] {

    override val logMalformed = false

    def reads(r: BSON.Reader): AiConfig = AiConfig(
      variant = draughts.variant.Variant orDefault (r int "v"),
      timeMode = TimeMode orDefault (r int "tm"),
      time = r double "t",
      increment = r int "i",
      days = r int "d",
      level = r int "l",
      color = Color.White,
      fen = r strO "f" filter (_.nonEmpty)
    )

    def writes(w: BSON.Writer, o: AiConfig) = $doc(
      "v" -> o.variant.id,
      "tm" -> o.timeMode.id,
      "t" -> o.time,
      "i" -> o.increment,
      "d" -> o.days,
      "l" -> o.level,
      "f" -> o.fen
    )
  }
}
