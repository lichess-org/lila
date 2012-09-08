package lila
package game

import user.User
import analyse.PgnDump
import csv.Writer
import controllers.routes

import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat, DateTimeFormatter }
import scalaz.effects._

final class Export(user: User, gameRepo: GameRepo) {

  val dateFormatter = ISODateTimeFormat.dateTime
  val baseUrl = "http://lichess.org/"

  // returns the web path
  def apply: IO[String] = for {
    games ← fetchGames
    filename = "%s_lichess_games_%s.csv".format(user.username, date)
    lines = header :: doGames(games).toList
    webPath ← Writer(filename)(lines)
  } yield webPath

  def doGames(rawGames: Iterator[RawDbGame]) = rawGames map (_.decode) collect {
    case Some(game) ⇒ doGame(game)
  }

  def doGame(game: DbGame) = {
    import game._
    val player = game player user
    List(
      id,
      dateFormatter.print(game.createdAt),
      player.fold(_.color.name, "?"),
      (player map game.opponent flatMap (_.userId)) | "anonymous",
      PgnDump result game,
      game.status,
      game.turns - 1,
      game.variant,
      game.mode,
      game.clock.fold(c ⇒ "%d %d".format(
        c.limitInMinutes, c.increment),
        "unlimited"),
      (player flatMap (_.elo)).fold(_.toString, "?"),
      (player flatMap (_.eloDiff)).fold(showEloDiff, "?"),
      (player map game.opponent flatMap (_.elo)).fold(_.toString, "?"),
      (player map game.opponent flatMap (_.eloDiff)).fold(showEloDiff, "?"),
      baseUrl + routes.Round.watcher(game.id, player.fold(_.color.name, "white")),
      baseUrl + routes.Analyse.replay(game.id, player.fold(_.color.name, "white")),
      baseUrl + routes.Analyse.pgn(game.id)
    )
  }

  def showEloDiff(n: Int): String = (n > 0).fold("+" + n, n.toString)

  def header =
    List("#", "Date (ISO8601)", "Color", "Opponent", "Result", "Status", "Plies", "Variant", "Mode", "Time control", "Your Elo", "Your Elo change", "Opponent Elo", "Opponent Elo Change", "Game url", "Analysis url", "PGN url") 

  def fetchGames = io {
    gameRepo.find(Query user user).sort(DBObject("createdAt" -> -1))
  }

  def date: String = (DateTimeFormat forPattern "yyyy-MM-dd") print new DateTime
}

object Export {

  def apply(gameRepo: GameRepo)(user: User) = new Export(user, gameRepo)
}
