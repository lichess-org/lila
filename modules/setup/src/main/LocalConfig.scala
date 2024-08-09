// package lila.setup

// import chess.format.FullFen
// import chess.{ Clock, ByColor }
// import chess.variant.Variant

// import scalalib.model.Days
// import lila.core.game.{ Game, IdGenerator, Player, Pov }
// import lila.lobby.Color
// //import lila.user.{ User, GameUser }
// import lila.rating.PerfType

// case class LocalConfig(
//     opponent: String,
//     timeMode: TimeMode,
//     time: Double,
//     increment: Clock.IncrementSeconds,
//     days: Days,
//     color: lila.lobby.TriColor,
//     fen: Option[FullFen] = None,
//     priv: Boolean = true
// ) extends Config
//     with Positional:

//   val variant   = Variant.default
//   val strictFen = true

//   def >> = (opponent, timeMode.id, time, increment, days, color.name, fen).some

//   private def game(user: GameUser)(using IdGenerator): Fu[Game] =
//     fenGame: chessGame =>
//       val pt = PerfType(chessGame.situation.board.variant, chess.Speed(chessGame.clock.map(_.config)))
//       Game
//         .make(
//           chess = chessGame,
//           players = ByColor: c =>
//             if creatorColor == c
//             then Player.make(c, user)
//             else Player.makeAnon(c, level.some),
//           mode = chess.Mode.Casual,
//           source = if chessGame.board.variant.fromPosition then Source.Position else Source.Ai,
//           daysPerTurn = makeDaysPerTurn,
//           pgnImport = None
//         )
//         .withUniqueId
//     .dmap(_.start)

//   def pov(user: GameUser)(using IdGenerator) = game(user) dmap { Pov(_, creatorColor) }

//   def timeControlFromPosition =
//     timeMode != TimeMode.RealTime || time >= 1

// object LocalConfig extends BaseConfig:

//   def from(
//       o: String,
//       tm: Int,
//       t: Double,
//       i: Clock.IncrementSeconds,
//       d: Days,
//       c: String,
//       fen: Option[FullFen],
//       priv: Boolean
//   ) =
//     new LocalConfig(
//       opponent = o,
//       timeMode = TimeMode(tm).err(s"Invalid time mode $tm"),
//       time = t,
//       increment = i,
//       days = d,
//       color = lila.lobby.Color('w').err("Invalid color " + c),
//       fen = fen,
//       priv = priv
//     )

//   val default = LocalConfig(
//     opponent = "coral",
//     timeMode = TimeMode.Unlimited,
//     time = 5d,
//     increment = Clock.IncrementSeconds(8),
//     days = Days(2),
//     color = lila.lobby.Color('2').err(""),
//     priv = true
//   )

//   import lila.db.BSON
//   import lila.db.dsl.{ *, given }

//   private[setup] given BSON[LocalConfig] with

//     def reads(r: BSON.Reader): LocalConfig =
//       LocalConfig(
//         opponent = r.get("o"),
//         timeMode = TimeMode.orDefault(r.int("tm")),
//         time = r.double("t"),
//         increment = r.get("i"),
//         days = r.get("d"),
//         color = Color.White,
//         fen = r.getO[FullFen]("f").filter(_.value.nonEmpty),
//         priv = r.bool("p")
//       )

//     def writes(w: BSON.Writer, o: LocalConfig) =
//       $doc(
//         "o"  -> o.opponent,
//         "tm" -> o.timeMode.id,
//         "t"  -> o.time,
//         "i"  -> o.increment,
//         "d"  -> o.days,
//         "f"  -> o.fen,
//         "p"  -> o.priv
//       )
