package lila.game

import chess.format.Forsyth
import chess.format.{ pgn ⇒ chessPgn }
import chess.format.pgn.{ Pgn, Tag }
import lila.user.User

import org.joda.time.format.DateTimeFormat

final class PgnDump(
    findUser: String ⇒ Fu[Option[User]]) {

  import PgnDump._

  def apply(gameUrl: String ⇒ String)(game: Game, pgn: String): Fu[Pgn] =
    tags(game, gameUrl) map { ts ⇒
      val fenSituation = ts find (_.name == Tag.FEN) flatMap { case Tag(_, fen) ⇒ Forsyth <<< fen }
      val pgn2 = (~fenSituation.map(_.situation.color.black)).fold(".. " + pgn, pgn)
      Pgn(ts, turns(pgn2, fenSituation.map(_.fullMoveNumber) | 1))
    }

  def filename(game: Game): Fu[String] = for {
    whiteUser ← game.whitePlayer.userId.zmap(findUser)
    blackUser ← game.blackPlayer.userId.zmap(findUser)
  } yield "lichess_pgn_%s_%s_vs_%s.%s.pgn".format(
    dateFormat.print(game.createdAt),
    player(game.whitePlayer, whiteUser),
    player(game.blackPlayer, blackUser),
    game.id)

  private val dateFormat = DateTimeFormat forPattern "yyyy-MM-dd";

  private def elo(p: Player) = p.elo.fold("?")(_.toString)

  private def player(p: Player, u: Option[User]) =
    p.aiLevel.fold(u.fold("Anonymous")(_.username))("AI level " + _)

  private def tags(game: Game, gameUrl: String ⇒ String): Fu[List[Tag]] = for {
    whiteUser ← game.whitePlayer.userId.zmap(findUser)
    blackUser ← game.blackPlayer.userId.zmap(findUser)
    initialFen ← game.variant.standard.fold(
      fuccess(none),
      GameRepo initialFen game.id)
  } yield List(
    Tag(_.Event, game.rated.fold("Rated game", "Casual game")),
    Tag(_.Site, gameUrl(game.id)),
    Tag(_.Date, dateFormat.print(game.createdAt)),
    Tag(_.White, player(game.whitePlayer, whiteUser)),
    Tag(_.Black, player(game.blackPlayer, blackUser)),
    Tag(_.Result, result(game)),
    Tag("WhiteElo", elo(game.whitePlayer)),
    Tag("BlackElo", elo(game.blackPlayer)),
    Tag("PlyCount", game.turns),
    Tag(_.Variant, game.variant.name.capitalize)
  ) ::: game.variant.standard.fold(Nil, List(
      Tag(_.FEN, initialFen | "?"),
      Tag("SetUp", "1")
    ))

  private def turns(pgn: String, from: Int): List[chessPgn.Turn] =
    (pgn split ' ' grouped 2).zipWithIndex.toList map {
      case (moves, index) ⇒ chessPgn.Turn(
        number = index + from,
        white = moves.headOption filter (".." !=) map { chessPgn.Move(_) },
        black = moves lift 1 map { chessPgn.Move(_) })
    }
}

object PgnDump {

  def result(game: Game) = game.finished.fold(
    game.winnerColor.fold("1/2-1/2")(color ⇒ color.white.fold("1-0", "0-1")),
    "*")
}
