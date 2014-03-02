package lila.game

import akka.pattern.ask
import org.joda.time.DateTime
import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat, DateTimeFormatter }

import lila.common.CsvServer
import lila.hub.actorApi.{ router => R }
import lila.user.User
import makeTimeout.short

private[game] final class Export(router: akka.actor.ActorSelection) {

  private val dateFormatter = ISODateTimeFormat.dateTime

  // returns the web path
  def apply(user: User): Fu[String] = for {
    games ← GameRepo recentByUser user.id
    data ← (games map doGame(user)).sequenceFu
    filename = "%s_lichess_games_%s.csv".format(user.username, date)
    webPath ← CsvServer(filename)(header :: data)
  } yield webPath

  private def doGame(user: User)(game: Game): Fu[List[String]] = {
    import game._
    val player = game player user
    (List(
      R.Watcher(game.id, player.fold("white")(_.color.name)),
      R.Pgn(game.id)
    ) map { r =>
        router ? R.Nolang(r) mapTo manifest[String]
      }).sequenceFu map { urls =>
        (List(
          id,
          dateFormatter.print(game.createdAt),
          player.fold("?")(_.color.name),
          (player map game.opponent flatMap (_.userId)) | "anonymous",
          PgnDump result game,
          game.status,
          game.turns - 1,
          game.variant,
          game.mode,
          game.clock.fold("unlimited") { c => "%d %d".format(c.limitInMinutes, c.increment) },
          (player flatMap (_.rating)).fold("?")(_.toString),
          (player flatMap (_.ratingDiff)).fold("?")(showRatingDiff),
          (player map game.opponent flatMap (_.rating)).fold("?")(_.toString),
          (player map game.opponent flatMap (_.ratingDiff)).fold("?")(showRatingDiff)
        ) map (_.toString)) ::: urls
      }
  }

  private def showRatingDiff(n: Int): String = (n > 0).fold("+" + n, n.toString)

  def header =
    List("#", "Date (ISO8601)", "Color", "Opponent", "Result", "Status", "Plies", "Variant", "Mode", "Time control", "Your Rating", "Your Rating change", "Opponent Rating", "Opponent Rating Change", "Game url", "PGN url")

  private def date: String = (DateTimeFormat forPattern "yyyy-MM-dd") print new DateTime
}
