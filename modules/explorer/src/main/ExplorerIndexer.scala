package lidraughts.explorer

import scala.util.Random.nextFloat
import scala.util.{ Try, Success, Failure }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.iteratee._
import play.api.libs.ws.WS
import play.api.Play.current
import draughts.format.pdn.Tag

import lidraughts.db.dsl._
import lidraughts.game.BSONHandlers.gameBSONHandler
import lidraughts.game.{ Game, GameRepo, Query, PdnDump, Player }
import lidraughts.user.UserRepo

private final class ExplorerIndexer(
    gameColl: Coll,
    internalEndpoint: String
) {

  private val maxGames = Int.MaxValue
  private val batchSize = 50
  private val separator = "\n\n\n"
  private val datePattern = "yyyy-MM-dd"
  private val dateFormatter = DateTimeFormat forPattern datePattern
  private val dateTimeFormatter = DateTimeFormat forPattern s"$datePattern HH:mm"
  private val pdnDateFormat = DateTimeFormat forPattern "yyyy.MM.dd";
  private val internalEndPointUrl = s"$internalEndpoint/import/lidraughts"

  private def parseDate(str: String): Option[DateTime] =
    Try(dateFormatter parseDateTime str).toOption

  type GamePDN = (Game, String)

  def apply(sinceStr: String): Funit =
    parseDate(sinceStr).fold(fufail[Unit](s"Invalid date $sinceStr")) { since =>
      logger.info(s"Start indexing since $since")
      val query =
        Query.createdSince(since) ++
          Query.rated ++
          Query.finished ++
          Query.turnsGt(8) ++
          Query.noProvisional ++
          Query.bothRatingsGreaterThan(1501)

      import reactivemongo.api._
      import reactivemongo.play.iteratees.cursorProducer

      gameColl.find(query)
        .sort(Query.sortChronological)
        .cursor[Game](ReadPreference.secondary)
        .enumerator(maxGames) &>
        Enumeratee.mapM[Game].apply[Option[GamePDN]] { game =>
          makeFastPdn(game) map {
            _ map { game -> _ }
          }
        } &>
        Enumeratee.collect { case Some(el) => el } &>
        Enumeratee.grouped(Iteratee takeUpTo batchSize) |>>>
        Iteratee.foldM[Seq[GamePDN], Long](nowMillis) {
          case (millis, pairs) =>
            WS.url(internalEndPointUrl).put(pairs.map(_._2) mkString separator).flatMap {
              case res if res.status == 200 =>
                val date = pairs.headOption.map(_._1.createdAt) ?? dateTimeFormatter.print
                val nb = pairs.size
                val gameMs = (nowMillis - millis) / nb.toDouble
                logger.info(s"$date $nb ${gameMs.toInt} ms/game ${(1000 / gameMs).toInt} games/s")
                funit
              case res => fufail(s"Stop import because of status ${res.status}")
            } >> {
              pairs.headOption match {
                case None => fufail(s"No games left, import complete!")
                case Some((g, _)) if (g.createdAt.isAfter(DateTime.now.minusMinutes(10))) =>
                  fufail(s"Found a recent game, import complete!")
                case _ => funit
              }
            } inject nowMillis
        } void
    }

  def apply(game: Game): Funit = makeFastPdn(game) map {
    _ foreach flowBuffer.apply
  }

  private object flowBuffer {
    private val max = 30
    private val buf = scala.collection.mutable.ArrayBuffer.empty[String]
    def apply(pdn: String): Unit = {
      buf += pdn
      val startAt = nowMillis
      if (buf.size >= max) {
        WS.url(internalEndPointUrl).put(buf mkString separator) andThen {
          case Success(res) if res.status == 200 =>
            lidraughts.mon.explorer.index.time(((nowMillis - startAt) / max).toInt)
            lidraughts.mon.explorer.index.success(max)
          case Success(res) =>
            logger.warn(s"[${res.status}]")
            lidraughts.mon.explorer.index.failure(max)
          case Failure(err) =>
            logger.warn(s"$err", err)
            lidraughts.mon.explorer.index.failure(max)
        }
        buf.clear
      }
    }
  }

  private def valid(game: Game) =
    game.finished &&
      game.rated &&
      game.turns >= 10 &&
      game.variant != draughts.variant.FromPosition

  private def stableRating(player: Player) = player.rating ifFalse player.provisional

  // probability of the game being indexed, between 0 and 1
  private def probability(game: Game, rating: Int) = {
    import lidraughts.rating.PerfType._
    game.perfType ?? {
      case Correspondence => 1
      case Rapid | Classical if rating >= 2000 => 1
      case Rapid | Classical if rating >= 1800 => 2 / 5f
      case Rapid | Classical => 1 / 8f
      case Blitz if rating >= 2000 => 1
      case Blitz if rating >= 1800 => 1 / 4f
      case Blitz => 1 / 15f
      case Bullet if rating >= 2300 => 1
      case Bullet if rating >= 2200 => 4 / 5f
      case Bullet if rating >= 2000 => 1 / 4f
      case Bullet if rating >= 1800 => 1 / 7f
      case Bullet => 1 / 20f
      case _ if rating >= 1600 => 1 // variant games
      case _ => 1 / 2f // noob variant games
    }
  }

  private def makeFastPdn(game: Game): Fu[Option[String]] = ~(for {
    whiteRating <- stableRating(game.whitePlayer)
    blackRating <- stableRating(game.blackPlayer)
    minPlayerRating = if (game.variant.exotic) 1400 else 1500
    minAverageRating = if (game.variant.exotic) 1520 else 1600
    if whiteRating >= minPlayerRating
    if blackRating >= minPlayerRating
    averageRating = (whiteRating + blackRating) / 2
    if averageRating >= minAverageRating
    if probability(game, averageRating) > nextFloat
    if valid(game)
  } yield GameRepo initialFen game flatMap { initialFen =>
    UserRepo.usernamesByIds(game.userIds) map { usernames =>
      def username(color: draughts.Color) = game.player(color).userId flatMap { id =>
        usernames.find(_.toLowerCase == id)
      } orElse game.player(color).userId getOrElse "?"
      val fenTags = initialFen.?? { fen => List(s"[FEN $fen]") }
      val timeControl = Tag.timeControl(game.clock.map(_.config)).value
      val otherTags = List(
        s"[LidraughtsID ${game.id}]",
        s"[Variant ${game.variant.name}]",
        s"[TimeControl $timeControl]",
        s"[White ${username(draughts.White)}]",
        s"[Black ${username(draughts.Black)}]",
        s"[WhiteElo $whiteRating]",
        s"[BlackElo $blackRating]",
        s"[Result ${PdnDump.result(game)}]",
        s"[Date ${pdnDateFormat.print(game.createdAt)}]"
      )
      val allTags = fenTags ::: otherTags
      s"${allTags.mkString("\n")}\n\n${game.pdnMoves.take(maxPlies).mkString(" ")}".some
    }
  })

  private val logger = lidraughts.log("explorer")
}
