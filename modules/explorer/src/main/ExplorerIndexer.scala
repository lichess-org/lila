package lila.explorer

import akka.stream.scaladsl.*
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.*
import scala.util.{ Failure, Success, Try }
import java.util.concurrent.atomic.AtomicReference

import lila.common.LilaStream
import lila.common.Json.given
import lila.db.dsl.{ *, given }
import lila.game.{ Game, GameRepo, Player }
import lila.user.{ User, UserRepo }

final private class ExplorerIndexer(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    getBotUserIds: lila.user.GetBotIds,
    ws: play.api.libs.ws.StandaloneWSClient,
    internalEndpoint: InternalEndpoint
)(using
    ec: Executor,
    mat: akka.stream.Materializer
):

  private val pgnDateFormat       = DateTimeFormat forPattern "yyyy.MM.dd"
  private val internalEndPointUrl = s"$internalEndpoint/import/lichess"

  def apply(game: Game): Funit =
    getBotUserIds() flatMap { botUserIds =>
      makeJson(game, botUserIds) map {
        _ foreach flowBuffer.apply
      }
    }

  private object flowBuffer:
    private val max       = 30
    private val bufferRef = new AtomicReference[Vector[JsObject]](Vector.empty)

    def apply(game: JsObject): Unit =
      if (bufferRef.updateAndGet(_ :+ game).size >= max)
        val buffer = bufferRef.getAndSet(Vector.empty)
        if (buffer.nonEmpty) {
          val startAt = nowMillis
          ws.url(internalEndPointUrl).put(JsArray(buffer)) andThen {
            case Success(res) if res.status == 200 =>
              lila.mon.explorer.index.time.record((nowMillis - startAt) / buffer.size)
              lila.mon.explorer.index.count(true).increment(buffer.size)
            case Success(res) =>
              logger.warn(s"[${res.status}]")
              lila.mon.explorer.index.count(false).increment(buffer.size)
            case Failure(err) =>
              logger.warn(s"$err", err)
              lila.mon.explorer.index.count(false).increment(buffer.size)
          } unit
        }

  private def valid(game: Game) =
    game.finished &&
      game.rated &&
      game.variant != chess.variant.FromPosition &&
      !Game.isOldHorde(game)

  // probability of the game being indexed, between 0 and 100
  private def probability(game: Game, rating: Int): Int =
    import lila.rating.PerfType.*
    game.perfType ?? {
      case Correspondence | Classical => 100

      case Rapid if rating >= 2200 => 100
      case Rapid if rating >= 2000 => 83
      case Rapid if rating >= 1800 => 46
      case Rapid if rating >= 1600 => 39
      case Rapid                   => 2

      case Blitz if rating >= 2500 => 100
      case Blitz if rating >= 2200 => 38
      case Blitz if rating >= 2000 => 18
      case Blitz if rating >= 1600 => 13
      case Blitz                   => 2

      case Bullet if rating >= 2500 => 100
      case Bullet if rating >= 2200 => 48
      case Bullet if rating >= 2000 => 27
      case Bullet if rating >= 1800 => 19
      case Bullet if rating >= 1600 => 18
      case Bullet                   => 2

      case UltraBullet => 100

      case _ if rating >= 1600 => 100 // variant games
      case _                   => 50  // noob variant games
    }

  private def makeJson(game: Game, botUserIds: Set[UserId]): Fu[Option[JsObject]] =
    ~(for {
      whiteRating <- game.whitePlayer.stableRating
      blackRating <- game.blackPlayer.stableRating
      if whiteRating >= 1501
      if blackRating >= 1501
      averageRating = (whiteRating + blackRating).value / 2
      if probability(game, averageRating) > (game.id.hashCode % 100)
      if !game.userIds.exists(botUserIds.contains)
      if valid(game)
    } yield gameRepo initialFen game flatMap { initialFen =>
      userRepo.usernamesByIds(game.userIds) map { usernames =>
        def username(color: chess.Color) =
          game.player(color).userId flatMap { id =>
            usernames.find(_.id == id)
          } orElse game.player(color).userId.map(_ into UserName)
        Json
          .obj(
            "id"      -> game.id,
            "variant" -> game.variant.key,
            "speed"   -> game.speed.key,
            "white" -> Json.obj(
              "name"   -> username(chess.White),
              "rating" -> whiteRating
            ),
            "black" -> Json.obj(
              "name"   -> username(chess.Black),
              "rating" -> blackRating
            ),
            "winner" -> game.winnerColor.map(_.name),
            "date"   -> pgnDateFormat.print(game.createdAt),
            "fen"    -> initialFen,
            "moves"  -> game.sans.mkString(" ")
          )
          .some
      }
    })

  private val logger = lila.log("explorer")
