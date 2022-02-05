package lila.explorer

import akka.stream.scaladsl._
import shogi.format.Tag
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import lila.common.ThreadLocalRandom.nextFloat
import scala.util.{ Failure, Success, Try }

import lila.common.LilaStream
import lila.db.dsl._
import lila.game.{ Game, GameRepo, NotationDump, Player, Query }
import lila.user.{ User, UserRepo }

final private class ExplorerIndexer(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    getBotUserIds: lila.user.GetBotIds,
    ws: play.api.libs.ws.WSClient,
    internalEndpoint: InternalEndpoint
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  private val separator           = "\n\n\n"
  private val datePattern         = "yyyy-MM-dd"
  private val dateFormatter       = DateTimeFormat forPattern datePattern
  private val internalEndPointUrl = s"$internalEndpoint/import/lishogi"

  private def parseDate(str: String): Option[DateTime] =
    Try(dateFormatter parseDateTime str).toOption

  def apply(sinceStr: String): Funit =
    getBotUserIds() flatMap { botUserIds =>
      parseDate(sinceStr).fold(fufail[Unit](s"Invalid date $sinceStr")) { since =>
        logger.info(s"Start indexing since $since")
        val query =
          Query.createdSince(since) ++
            Query.rated ++
            Query.finished ++
            Query.pliesGt(8) ++
            Query.noProvisional ++
            Query.bothRatingsGreaterThan(1501)

        gameRepo
          .sortedCursor(query, Query.sortChronological)
          .documentSource()
          .via(LilaStream.logRate[Game]("fetch")(logger))
          .mapAsyncUnordered(8) { makeFastNotation(_, botUserIds) }
          .via(LilaStream.collect)
          .via(LilaStream.logRate("index")(logger))
          .grouped(50)
          .map(_ mkString separator)
          .mapAsyncUnordered(2) { notation =>
            ws.url(internalEndPointUrl).put(notation).flatMap {
              case res if res.status == 200 => funit
              case res                      => fufail(s"Stop import because of status ${res.status}")
            }
          }
          .toMat(Sink.ignore)(Keep.right)
          .run()
          .void
      }
    }

  def apply(game: Game): Funit =
    getBotUserIds() flatMap { botUserIds =>
      makeFastNotation(game, botUserIds) map {
        _ foreach flowBuffer.apply
      }
    }

  private object flowBuffer {
    private val max = 30
    private val buf = scala.collection.mutable.ArrayBuffer.empty[String]
    def apply(notation: String): Unit = {
      buf += notation
      val startAt = nowMillis
      if (buf.size >= max) {
        ws.url(internalEndPointUrl).put(buf mkString separator) andThen {
          case Success(res) if res.status == 200 =>
            lila.mon.explorer.index.time.record((nowMillis - startAt) / max)
            lila.mon.explorer.index.count(true).increment(max)
          case Success(res) =>
            logger.warn(s"[${res.status}]")
            lila.mon.explorer.index.count(false).increment(max)
          case Failure(err) =>
            logger.warn(s"$err", err)
            lila.mon.explorer.index.count(false).increment(max)
        }
        buf.clear()
      }
    }
  }

  private def valid(game: Game) =
    game.finished &&
      game.rated &&
      game.plies >= 10 &&
      game.initialSfen.isEmpty

  private def stableRating(player: Player) = player.rating ifFalse player.provisional

  // probability of the game being indexed, between 0 and 1
  private def probability(game: Game, rating: Int) = {
    import lila.rating.PerfType._
    game.perfType ?? {
      case Correspondence                      => 1
      case Rapid | Classical if rating >= 2000 => 1
      case Rapid | Classical if rating >= 1800 => 2 / 5f
      case Rapid | Classical                   => 1 / 8f
      case Blitz if rating >= 2000             => 1
      case Blitz if rating >= 1800             => 1 / 4f
      case Blitz                               => 1 / 15f
      case Bullet if rating >= 2300            => 1
      case Bullet if rating >= 2200            => 4 / 5f
      case Bullet if rating >= 2000            => 1 / 4f
      case Bullet if rating >= 1800            => 1 / 7f
      case Bullet                              => 1 / 20f
      case _ if rating >= 1600                 => 1      // variant games
      case _                                   => 1 / 2f // noob variant games
    }
  }

  // todo...
  private def makeFastNotation(game: Game, botUserIds: Set[User.ID]): Fu[Option[String]] =
    ~(for {
      senteRating <- stableRating(game.sentePlayer)
      goteRating  <- stableRating(game.gotePlayer)
      minPlayerRating  = if (!game.variant.standard) 1400 else 1500
      minAverageRating = if (!game.variant.standard) 1520 else 1600
      if senteRating >= minPlayerRating
      if goteRating >= minPlayerRating
      averageRating = (senteRating + goteRating) / 2
      if averageRating >= minAverageRating
      if probability(game, averageRating) > nextFloat()
      if !game.userIds.exists(botUserIds.contains)
      if valid(game)
    } yield userRepo.usernamesByIds(game.userIds) map { usernames =>
        def username(color: shogi.Color) =
          game.player(color).userId flatMap { id =>
            usernames.find(_.toLowerCase == id)
          } orElse game.player(color).userId getOrElse "?"
        val sfenTags = game.initialSfen.?? { sfen =>
          List(s"$$SFEN:$sfen]")
        }
        val timeControl = Tag.timeControlCsa(game.clock.map(_.config)).value
        val otherTags = List(
          s"N+${username(shogi.Sente)}",
          s"N-${username(shogi.Gote)}",
          s"$$LishogiID:${game.id}",
          s"$$Variant:${game.variant.name}",
          s"[TimeControl $timeControl",
          s"$$SenteElo $senteRating",
          s"$$GoteElo $goteRating",
          s"$$Result ${NotationDump.result(game)}",
          s"$$Start${dateFormatter.print(game.createdAt)}"
        )
        val allTags = sfenTags ::: otherTags
        s"${allTags.mkString("\n")}\n\n${game.usiMoves.take(maxPlies).map(_.usi).mkString(" ")}".some
      }
    )

  private val logger = lila.log("explorer")
}
