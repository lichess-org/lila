package lila.game

import akka.actor._
import akka.pattern.ask
import chess.format.Forsyth
import chess.format.pgn.{ Pgn, Tag }
import chess.format.{ pgn => chessPgn }
import makeTimeout.short
import org.joda.time.format.DateTimeFormat

import lila.hub.actorApi.router.{ Abs, Watcher }
import lila.user.User

final class PgnDump(
    router: ActorSelection,
    findUser: String => Fu[Option[User]]) {

  import PgnDump._

  def apply(game: Game): Fu[Pgn] =
    tags(game) map { ts =>
      val fenSituation = ts find (_.name == Tag.FEN) flatMap { case Tag(_, fen) => Forsyth <<< fen }
      val moves2 = (~fenSituation.map(_.situation.color.black)).fold(".." :: game.pgnMoves, game.pgnMoves)
      Pgn(ts, turns(moves2, fenSituation.map(_.fullMoveNumber) | 1))
    }

  def filename(game: Game): Fu[String] = gameUsers(game) map {
    case (wu, bu) => "lichess_pgn_%s_%s_vs_%s.%s.pgn".format(
      dateFormat.print(game.createdAt),
      player(game.whitePlayer, wu),
      player(game.blackPlayer, bu),
      game.id)
  }

  private def gameUrl(id: String): Fu[String] =
    router ? Abs(Watcher(id, "white")) mapTo manifest[String]

  private def gameUsers(game: Game): Fu[(Option[User], Option[User])] =
    (game.whitePlayer.userId ?? findUser) zip (game.blackPlayer.userId ?? findUser)

  private val dateFormat = DateTimeFormat forPattern "yyyy.MM.dd";

  private def rating(p: Player) = p.rating.fold("?")(_.toString)

  private def player(p: Player, u: Option[User]) =
    p.aiLevel.fold(u.fold(User.anonymous)(_.username))("lichess AI level " + _)

  private def tags(game: Game): Fu[List[Tag]] = {
    val opening =
      if (game.fromPosition || game.variant.exotic) none
      else chess.OpeningExplorer openingOf game.pgnMoves
    gameUsers(game) zip
      (game.variant.standard.fold(fuccess(none), GameRepo initialFen game.id)) zip
      gameUrl(game.id) map {
        case ((((wu, bu)), initialFen), url) => List(
          Tag(_.Event, game.rated.fold("Rated game", "Casual game")),
          Tag(_.Site, url),
          Tag(_.Date, dateFormat.print(game.createdAt)),
          Tag(_.White, player(game.whitePlayer, wu)),
          Tag(_.Black, player(game.blackPlayer, bu)),
          Tag(_.Result, result(game)),
          Tag("WhiteElo", rating(game.whitePlayer)),
          Tag("BlackElo", rating(game.blackPlayer)),
          Tag("PlyCount", game.turns),
          Tag(_.Variant, game.variant.name.capitalize),
          Tag(_.TimeControl, game.clock.fold("-") { c => s"${c.limit}+${c.increment}" }),
          Tag(_.ECO, opening.fold("?")(_.code)),
          Tag(_.Opening, opening.fold("?")(_.name))
        ) ::: game.variant.standard.fold(Nil, List(
            Tag(_.FEN, initialFen | "?"),
            Tag("SetUp", "1")
          ))
      }
  }

  private def turns(moves: List[String], from: Int): List[chessPgn.Turn] =
    (moves grouped 2).zipWithIndex.toList map {
      case (moves, index) => chessPgn.Turn(
        number = index + from,
        white = moves.headOption filter (".." !=) map { chessPgn.Move(_) },
        black = moves lift 1 map { chessPgn.Move(_) })
    } filterNot (_.isEmpty)
}

object PgnDump {

  def result(game: Game) = game.finished.fold(
    game.winnerColor.fold("1/2-1/2")(color => color.white.fold("1-0", "0-1")),
    "*")
}
