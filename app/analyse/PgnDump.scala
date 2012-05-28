package lila
package analyse

import chess.format.Forsyth
import game.{ DbGame, DbPlayer, GameRepo }
import user.{ User, UserRepo }

import org.joda.time.format.DateTimeFormat
import scalaz.effects._

final class PgnDump(gameRepo: GameRepo, userRepo: UserRepo) {

  val dateFormat = DateTimeFormat forPattern "yyyy-MM-dd";

  def >>(game: DbGame): IO[String] =
    header(game) map { headers ⇒
      "%s\n\n%s %s".format(headers, moves(game), result(game))
    }

  def header(game: DbGame): IO[String] = for {
    whiteUser ← user(game.whitePlayer)
    blackUser ← user(game.blackPlayer)
    initialFen ← game.variant.standard.fold(io(none), gameRepo initialFen game.id)
  } yield List(
    "Event" -> game.rated.fold("Rated game", "Casual game"),
    "Site" -> ("http://lichess.org/" + game.id),
    "Date" -> game.createdAt.fold(dateFormat.print, "?"),
    "White" -> player(game.whitePlayer, whiteUser),
    "Black" -> player(game.blackPlayer, blackUser),
    "WhiteElo" -> elo(game.whitePlayer),
    "BlackElo" -> elo(game.blackPlayer),
    "Result" -> result(game),
    "PlyCount" -> game.turns,
    "Variant" -> game.variant.name
  ) ++ game.variant.standard.fold(Map.empty, Map(
      "FEN" -> (initialFen | "?"),
      "SetUp" -> "1"
    )) map {
      case (name, value) ⇒ """[%s "%s"]""".format(name, value)
    } mkString "\n"

  def elo(p: DbPlayer) = p.elo.fold(_.toString, "?")

  def user(p: DbPlayer): IO[Option[User]] = p.userId.fold(
    userRepo.byId,
    io(none))

  def player(p: DbPlayer, u: Option[User]) = p.aiLevel.fold(
    "Crafty level " + _,
    u.fold(_.username, "Anonymous"))

  def moves(game: DbGame) = (game.pgnList grouped 2).zipWithIndex map {
    case (moves, turn) ⇒ "%d. %s".format((turn + 1), moves.mkString(" "))
  } mkString " "

  def result(game: DbGame) = game.finished.fold(
    game.winnerColor.fold(
      color ⇒ color.white.fold("1-0", "0-1"),
      "1/2-1/2"),
    "*")
}
