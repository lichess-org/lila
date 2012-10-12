package lila
package analyse

import chess.format.Forsyth
import chess.format.{ pgn => chessPgn }
import chess.format.pgn.{ Pgn, Tag }
import game.{ DbGame, DbPlayer, GameRepo }
import user.{ User, UserRepo }

import org.joda.time.format.DateTimeFormat
import scalaz.effects._

final class PgnDump(
    gameRepo: GameRepo,
    analyser: Analyser,
    userRepo: UserRepo) {

  import PgnDump._

  def apply(game: DbGame, pgn: String): IO[Pgn] = for {
    ts ← tags(game)
    pgnObj = Pgn(ts, turns(pgn))
    analysis ← analyser get game.id
  } yield analysis.fold(Annotator(pgnObj, _), pgnObj)

  def filename(game: DbGame): IO[String] = for {
    whiteUser ← user(game.whitePlayer)
    blackUser ← user(game.blackPlayer)
  } yield "lichess_pgn_%s_%s_vs_%s.%s.pgn".format(
    dateFormat.print(game.createdAt),
    player(game.whitePlayer, whiteUser),
    player(game.blackPlayer, blackUser),
    game.id)

  private val baseUrl = "http://lichess.org/"
  private val dateFormat = DateTimeFormat forPattern "yyyy-MM-dd";

  private def elo(p: DbPlayer) = p.elo.fold(_.toString, "?")

  private def user(p: DbPlayer): IO[Option[User]] = p.userId.fold(
    userRepo.byId,
    io(none))

  private def player(p: DbPlayer, u: Option[User]) = p.aiLevel.fold(
    "AI level " + _,
    u.fold(_.username, "Anonymous"))

  private def tags(game: DbGame): IO[List[Tag]] = for {
    whiteUser ← user(game.whitePlayer)
    blackUser ← user(game.blackPlayer)
    initialFen ← game.variant.standard.fold(io(none), gameRepo initialFen game.id)
  } yield List(
    Tag(_.Event, game.rated.fold("Rated game", "Casual game")),
    Tag(_.Site, baseUrl + game.id),
    Tag(_.Date, dateFormat.print(game.createdAt)),
    Tag(_.White, player(game.whitePlayer, whiteUser)),
    Tag(_.Black, player(game.blackPlayer, blackUser)),
    Tag(_.Result, result(game)),
    Tag("WhiteElo", elo(game.whitePlayer)),
    Tag("BlackElo", elo(game.blackPlayer)),
    Tag("PlyCount", game.turns),
    Tag(_.Variant, game.variant.name)
  ) ::: game.variant.standard.fold(Nil, List(
      Tag(_.FEN, initialFen | "?"),
      Tag("SetUp", "1")
    ))

  private def turns(pgn: String): List[chessPgn.Turn] =
    (pgn split ' ' grouped 2).zipWithIndex.toList map {
      case (moves, index) ⇒ chessPgn.Turn(
        number = index + 1,
        white = moves.headOption map { chessPgn.Move(_) },
        black = moves.tail.headOption map { chessPgn.Move(_) })
    }
}

object PgnDump {

  def result(game: DbGame) = game.finished.fold(
    game.winnerColor.fold(
      color ⇒ color.white.fold("1-0", "0-1"),
      "1/2-1/2"),
    "*")
}
