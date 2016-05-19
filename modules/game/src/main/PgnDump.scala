package lila.game

import chess.format.Forsyth
import chess.format.pgn.{ Pgn, Tag, Parser, ParsedPgn }
import chess.format.{ pgn => chessPgn }
import org.joda.time.format.DateTimeFormat

import lila.common.LightUser

final class PgnDump(
    netBaseUrl: String,
    getLightUser: String => Option[LightUser]) {

  import PgnDump._

  def apply(game: Game, initialFen: Option[String]): Pgn = {
    val imported = game.pgnImport.flatMap { pgni =>
      Parser.full(pgni.pgn).toOption
    }
    val ts = tags(game, initialFen, imported)
    val fenSituation = ts find (_.name == Tag.FEN) flatMap { case Tag(_, fen) => Forsyth <<< fen }
    val moves2 = fenSituation.??(_.situation.color.black).fold(".." :: game.pgnMoves, game.pgnMoves)
    Pgn(ts, turns(moves2, fenSituation.map(_.fullMoveNumber) | 1))
  }

  private val fileR = """[\s,]""".r

  def filename(game: Game): String = gameLightUsers(game) match {
    case (wu, bu) => fileR.replaceAllIn(
      "lichess_pgn_%s_%s_vs_%s.%s.pgn".format(
        dateFormat.print(game.createdAt),
        player(game.whitePlayer, wu),
        player(game.blackPlayer, bu),
        game.id
      ), "_")
  }

  private def gameUrl(id: String) = s"$netBaseUrl/$id"

  private def gameLightUsers(game: Game): (Option[LightUser], Option[LightUser]) =
    (game.whitePlayer.userId ?? getLightUser) -> (game.blackPlayer.userId ?? getLightUser)

  private val dateFormat = DateTimeFormat forPattern "yyyy.MM.dd";

  private def rating(p: Player) = p.rating.fold("?")(_.toString)

  private def player(p: Player, u: Option[LightUser]) =
    p.aiLevel.fold(u.fold(p.name | lila.user.User.anonymous)(_.name))("lichess AI level " + _)

  private val customStartPosition: Set[chess.variant.Variant] =
    Set(chess.variant.Chess960, chess.variant.FromPosition, chess.variant.Horde, chess.variant.RacingKings)

  def tags(
    game: Game,
    initialFen: Option[String],
    imported: Option[ParsedPgn]): List[Tag] = gameLightUsers(game) match {
    case (wu, bu) => List(
      Tag(_.Event, imported.flatMap(_ tag "event") | {
        if (game.imported) "Import"
        else game.rated.fold("Rated game", "Casual game")
      }),
      Tag(_.Site, gameUrl(game.id)),
      Tag(_.Date, imported.flatMap(_ tag "date") | dateFormat.print(game.createdAt)),
      Tag(_.White, player(game.whitePlayer, wu)),
      Tag(_.Black, player(game.blackPlayer, bu)),
      Tag(_.Result, result(game)),
      Tag("WhiteElo", rating(game.whitePlayer)),
      Tag("BlackElo", rating(game.blackPlayer)),
      Tag("PlyCount", game.turns),
      Tag(_.Variant, game.variant.name.capitalize),
      Tag(_.TimeControl, game.clock.fold("-") { c => s"${c.limit}+${c.increment}" }),
      Tag(_.ECO, game.opening.fold("?")(_.opening.eco)),
      Tag(_.Opening, game.opening.fold("?")(_.opening.name)),
      Tag(_.Termination, {
        import chess.Status._
        game.status match {
          case Created | Started                             => "Unterminated"
          case Aborted | NoStart                             => "Abandoned"
          case Timeout | Outoftime                           => "Time forfeit"
          case Resign | Draw | Stalemate | Mate | VariantEnd => "Normal"
          case Cheat                                         => "Rules infraction"
          case UnknownFinish                                 => "Unknown"
        }
      })
    ) ::: customStartPosition(game.variant).??(List(
        Tag(_.FEN, initialFen | "?"),
        Tag("SetUp", "1")
      ))
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
