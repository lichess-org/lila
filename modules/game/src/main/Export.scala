package lila.game

import lila.user.User
import lila.common.CsvServer

import org.joda.time.DateTime
import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat, DateTimeFormatter }

// TODO stream that shit
// private[game] final class Export(user: User, gameRepo: GameRepo) {

//   private val dateFormatter = ISODateTimeFormat.dateTime

//   // returns the web path
//   def apply: Fu[String] = for {
//     games ← fetchGames
//     filename = "%s_lichess_games_%s.csv".format(user.username, date)
//     lines = header :: doGames(games).toList
//     webPath ← CsvServer(filename)(lines)
//   } yield webPath

//   def doGames(rawGames: Iterator[RawDbGame]) = rawGames map (_.decode) collect {
//     case Some(game) ⇒ doGame(game)
//   }

//   private def doGame(game: DbGame) = {
//     import game._
//     val player = game player user
//     List(
//       id,
//       dateFormatter.print(game.createdAt),
//       player.fold("?")(_.color.name),
//       (player map game.opponent flatMap (_.userId)) | "anonymous",
//       PgnDump result game,
//       game.status,
//       game.turns - 1,
//       game.variant,
//       game.mode,
//       game.clock.fold("unlimited") { c ⇒ "%d %d".format(c.limitInMinutes, c.increment) },
//       (player flatMap (_.elo)).fold("?")(_.toString),
//       (player flatMap (_.eloDiff)).fold("?")(showEloDiff),
//       (player map game.opponent flatMap (_.elo)).fold("?")(_.toString),
//       (player map game.opponent flatMap (_.eloDiff)).fold("?")(showEloDiff),
//       netBaseUrl + routes.Round.watcher(game.id, player.fold("white")(_.color.name)),
//       netBaseUrl + routes.Analyse.replay(game.id, player.fold("white")(_.color.name)),
//       netBaseUrl + routes.Analyse.pgn(game.id)
//     )
//   }

//   private def showEloDiff(n: Int): String = (n > 0).fold("+" + n, n.toString)

//   def header =
//     List("#", "Date (ISO8601)", "Color", "Opponent", "Result", "Status", "Plies", "Variant", "Mode", "Time control", "Your Elo", "Your Elo change", "Opponent Elo", "Opponent Elo Change", "Game url", "Analysis url", "PGN url") 

//   private def fetchGames = gameRepo recentByUser user.id

//   private def date: String = (DateTimeFormat forPattern "yyyy-MM-dd") print new DateTime
// }

// object Export {

//   def apply(gameRepo: GameRepo)(user: User) = new Export(user, gameRepo)
// }
