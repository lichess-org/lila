package lila.explorer

import scala.util.{ Success, Failure }

import chess.variant.Variant
import play.api.libs.iteratee._
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.api.Play.current

import lila.db.api._
import lila.db.Implicits._
import lila.game.BSONHandlers.gameBSONHandler
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo, Query, PgnDump, Player }

private final class ExplorerIndexer(endpoint: String) {

  val maxGames = Int.MaxValue
  val batchSize = 100
  val separator = "\n\n\n"

  def stableRating(player: Player) = player.rating ifFalse player.provisional

  private def makeFastPgn(game: Game): Fu[Option[String]] = ~(for {
    whiteRating <- stableRating(game.whitePlayer)
    blackRating <- stableRating(game.blackPlayer)
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
        Iteratee.foldM[Seq[Option[String]], Int](0) {
          case (number, pgnOptions) =>
            val pgns = pgnOptions.flatten
            WS.url(url).put(pgns mkString separator pp) andThen {
              case Success(res) if res.status == 200 => logger.info(number.toString)
              case Success(res)                      => logger.warn(s"[${res.status}]")
              case Failure(err)                      => logger.warn(s"$err")
            } inject (number + pgns.size)
        } void
  }
}
