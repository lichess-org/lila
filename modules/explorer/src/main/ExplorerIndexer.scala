package lila.explorer

import scala.util.Random.nextFloat
import scala.util.{ Try, Success, Failure }

import chess.variant.Variant
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.iteratee._
import play.api.libs.ws.WS
import play.api.Play.current

import lila.db.api._
import lila.db.Implicits._
import lila.game.BSONHandlers.gameBSONHandler
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo, Query, PgnDump, Player }
import lila.user.UserRepo

private final class ExplorerIndexer(
    endpoint: String,
    massImportEndpoint: String) {

  private val maxGames = Int.MaxValue
  private val batchSize = 100
  private val maxPlies = 50
  private val separator = "\n\n\n"
  private val datePattern = "yyyy-MM-dd"
  private val dateFormatter = DateTimeFormat forPattern datePattern
  private val dateTimeFormatter = DateTimeFormat forPattern s"$datePattern HH:mm"
  private val pgnDateFormat = DateTimeFormat forPattern "yyyy.MM.dd";
  private val endPointUrl = s"$endpoint/import/lichess"
  private val massImportEndPointUrl = s"$massImportEndpoint/import/lichess"

  private def parseDate(str: String): Option[DateTime] =
    Try(dateFormatter parseDateTime str).toOption

  def apply(sinceStr: String): Funit =
    parseDate(sinceStr).fold(fufail[Unit](s"Invalid date $sinceStr")) { since =>
      logger.info(s"Start indexing since $since")
      val query = $query(
        Query.createdSince(since) ++
          Query.rated ++
          Query.finished ++
          Query.turnsMoreThan(8) ++
          Query.noProvisional ++
          Query.bothRatingsGreaterThan(1501)
      )
      pimpQB(query)
        .sort(Query.sortChronological)
        .cursor[Game]()
        .enumerate(maxGames, stopOnError = true) &>
        Enumeratee.mapM[Game].apply[Option[(Game, String)]] { game =>
          makeFastPgn(game) map {
            _ map { game -> _ }
          }
        } &>
        Enumeratee.grouped(Iteratee takeUpTo batchSize) |>>>
        Iteratee.foldM[Seq[Option[(Game, String)]], Long](nowMillis) {
          case (millis, pairOptions) =>
            val pairs = pairOptions.flatten
            WS.url(massImportEndPointUrl).put(pairs.map(_._2) mkString separator).andThen {
              case Success(res) if res.status == 200 =>
                val date = pairs.headOption.map(_._1.createdAt) ?? dateTimeFormatter.print
                val nb = pairs.size
                val gameMs = (nowMillis - millis) / nb.toDouble
                logger.info(s"$date $nb/$batchSize ${gameMs.toInt} ms/game ${(1000 / gameMs).toInt} games/s")
              case Success(res) => logger.warn(s"[${res.status}]")
              case Failure(err) => logger.warn(s"$err")
            } >>- {
              if (pairOptions.size < batchSize)
                sys error s"Got ${pairOptions.size}/$batchSize games, stopping import"
            } inject nowMillis
        } void
    }

  def apply(game: Game): Funit = makeFastPgn(game) map {
    _ foreach flowBuffer.apply
  }

  private object flowBuffer {
    private val max = 30
    private val buf = scala.collection.mutable.ArrayBuffer.empty[String]
    def apply(pgn: String) {
      buf += pgn
      val startAt = nowMillis
      if (buf.size >= max) {
        WS.url(endPointUrl).put(buf mkString separator) andThen {
          case Success(res) if res.status == 200 =>
            val gameMs = (nowMillis - startAt) / max
            logger.info(s"indexed $max games at ${gameMs.toInt} ms/game")
          case Success(res) => logger.warn(s"[${res.status}]")
          case Failure(err) => logger.warn(s"$err")
        }
        buf.clear
      }
    }
  }

  private def valid(game: Game) =
    game.finished &&
      game.rated &&
      game.turns >= 10 &&
      game.variant != chess.variant.FromPosition &&
      (game.variant != chess.variant.Horde || game.createdAt.isAfter(Query.hordeWhitePawnsSince))

  private def stableRating(player: Player) = player.rating ifFalse player.provisional

  // probability of the game being indexed, between 0 and 1
  private def probability(game: Game, rating: Int) = {
    import lila.rating.PerfType._
    game.perfType ?? {
      case Correspondence              => 1
      case Classical if rating >= 2000 => 1
      case Classical if rating >= 1800 => 2 / 5f
      case Classical                   => 1 / 12f
      case Blitz if rating >= 2000     => 1
      case Blitz if rating >= 1800     => 1 / 8f
      case Blitz                       => 1 / 20f
      case Bullet if rating >= 2200    => 1
      case Bullet if rating >= 2000    => 1 / 3f
      case Bullet if rating >= 1800    => 1 / 10f
      case Bullet                      => 1 / 15f
      case _ if rating >= 1600         => 1 // variant games
      case _                           => 1 / 2f // noob variant games
    }
  }

  private def makeFastPgn(game: Game): Fu[Option[String]] = ~(for {
    whiteRating <- stableRating(game.whitePlayer)
    blackRating <- stableRating(game.blackPlayer)
    minPlayerRating = if (game.variant.exotic) 1400 else 1500
    minAverageRating = if (game.variant.exotic) 1550 else 1600
    if whiteRating >= minPlayerRating
    if blackRating >= minPlayerRating
    averageRating = (whiteRating + blackRating) / 2
    if averageRating >= minAverageRating
    if probability(game, averageRating) > nextFloat
    if valid(game)
  } yield GameRepo initialFen game flatMap { initialFen =>
    UserRepo.usernamesByIds(game.userIds) map { usernames =>
      def username(color: chess.Color) = game.player(color).userId flatMap { id =>
        usernames.find(_.toLowerCase == id)
      } orElse game.player(color).userId getOrElse "?"
      val fenTags = initialFen.?? { fen => List(s"[FEN $fen]") }
      val timeControl = game.clock.fold("-") { c => s"${c.limit}+${c.increment}" }
      val otherTags = List(
        s"[LichessID ${game.id}]",
        s"[Variant ${game.variant.name}]",
        s"[TimeControl $timeControl]",
        s"[White ${username(chess.White)}]",
        s"[Black ${username(chess.Black)}]",
        s"[WhiteElo $whiteRating]",
        s"[BlackElo $blackRating]",
        s"[Result ${PgnDump.result(game)}]",
        s"[Date ${pgnDateFormat.print(game.createdAt)}]")
      val allTags = fenTags ::: otherTags
      s"${allTags.mkString("\n")}\n\n${game.pgnMoves.take(maxPlies).mkString(" ")}".some
    }
  })

  private val logger = play.api.Logger("explorer")
}
