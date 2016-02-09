package lila.explorer

import scala.util.{ Success, Failure }

import chess.variant.Variant
import org.joda.time.DateTime
import play.api.libs.iteratee._
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

import lila.db.api._
import lila.db.Implicits._
import lila.game.BSONHandlers.gameBSONHandler
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo, Query, PgnDump, Player }

private final class ExplorerIndexer(endpoint: String) {

  private val maxGames = Int.MaxValue
  private val batchSize = 100
  private val minRating = 1600
  private val separator = "\n\n\n"

  def apply(variantKey: String): Funit = Variant.byKey get variantKey match {
    case None => fufail(s"Invalid variant $variantKey")
    case Some(variant) =>
      val url = s"$endpoint/lichess/${variant.key}"
      val query = $query(
        Query.rated ++
          Query.finished ++
          Query.turnsMoreThan(10) ++
          Query.variant(variant) ++
          (variant == chess.variant.Horde).??(Query.sinceHordePawnsAreWhite))
      pimpQB(query)
        .sort(Query.sortChronological)
        .cursor[Game]()
        .enumerate(maxGames, stopOnError = true) &>
        Enumeratee.mapM[Game].apply[Option[String]](makeFastPgn) &>
        Enumeratee.grouped(Iteratee takeUpTo batchSize) |>>>
        Iteratee.foldM[Seq[Option[String]], (Int, Long)](0 -> nowMillis) {
          case ((number, millis), pgnOptions) =>
            val pgns = pgnOptions.flatten
            WS.url(url).put(pgns mkString separator) andThen {
              case Success(res) if res.status == 200 => logger.info(s"${number} ${nowMillis - millis} ms")
              case Success(res)                      => logger.warn(s"[${res.status}]")
              case Failure(err)                      => logger.warn(s"$err")
            } inject {
              (number + pgns.size) -> nowMillis
            }
        } void
  }

  def apply(game: Game): Funit = makeFastPgn(game).flatMap {
    _ ?? { pgn =>
      val url = s"$endpoint/lichess/${game.variant.key}"
      WS.url(url).put(pgn) andThen {
        case Success(res) if res.status == 200 =>
        case Success(res)                      => logger.warn(s"[${res.status}]")
        case Failure(err)                      => logger.warn(s"$err")
      } void
    }
  }

  private def valid(game: Game) =
    game.finished &&
      game.rated &&
      game.turns >= 10 &&
      game.variant != chess.variant.FromPosition

  private def stableRating(player: Player) = player.rating ifFalse player.provisional

  private def makeFastPgn(game: Game): Fu[Option[String]] = ~(for {
    whiteRating <- stableRating(game.whitePlayer)
    blackRating <- stableRating(game.blackPlayer)
    if ((whiteRating + blackRating) / 2 > minRating)
    if valid(game)
  } yield GameRepo initialFen game map { initialFen =>
    val fenTags = initialFen.?? { fen => List(s"[FEN $fen]") }
    val otherTags = List(
      s"[LichessID ${game.id}]",
      s"[TimeControl ${game.clock.fold("-")(_.show)}]",
      s"[WhiteElo $whiteRating]",
      s"[BlackElo $blackRating]",
      s"[Result ${PgnDump.result(game)}]")
    val allTags = fenTags ::: otherTags
    s"${allTags.mkString("\n")}\n\n${game.pgnMoves.mkString(" ")}".some
  })

  private val logger = play.api.Logger("explorer")
}
