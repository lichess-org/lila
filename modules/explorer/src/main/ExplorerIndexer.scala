package lila.explorer

import akka.stream.scaladsl._
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import play.api.libs.ws.JsonBodyWritables._
import scala.util.{ Failure, Success, Try }

import lila.common.LilaStream
import lila.db.dsl._
import lila.game.{ Game, GameRepo, Player }
import lila.user.{ User, UserRepo }

final private class ExplorerIndexer(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    getBotUserIds: lila.user.GetBotIds,
    ws: play.api.libs.ws.StandaloneWSClient,
    internalEndpoint: InternalEndpoint
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  private val pgnDateFormat       = DateTimeFormat forPattern "yyyy.MM.dd"
  private val internalEndPointUrl = s"$internalEndpoint/import/lichess"

  def apply(game: Game): Funit =
    getBotUserIds() flatMap { botUserIds =>
      makeJson(game, botUserIds) map {
        _ foreach flowBuffer.apply
      }
    }

  private object flowBuffer {
    private val max = 30
    private val buf = scala.collection.mutable.ArrayBuffer.empty[JsObject]
    def apply(game: JsObject): Unit = {
      buf += game
      val startAt = nowMillis
      if (buf.sizeIs >= max) {
        ws.url(internalEndPointUrl).put(JsArray(buf)) andThen {
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
      game.variant != chess.variant.FromPosition &&
      !Game.isOldHorde(game)

  private def stableRating(player: Player) = player.rating ifFalse player.provisional

  // probability of the game being indexed, between 0 and 100
  private def probability(game: Game, rating: Int): Int = {
    import lila.rating.PerfType._
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
  }

  private def makeJson(game: Game, botUserIds: Set[User.ID]): Fu[Option[JsObject]] =
    ~(for {
      whiteRating <- stableRating(game.whitePlayer)
      blackRating <- stableRating(game.blackPlayer)
      if whiteRating >= 1501
      if blackRating >= 1501
      averageRating = (whiteRating + blackRating) / 2
      if probability(game, averageRating) > (game.id.hashCode % 100)
      if !game.userIds.exists(botUserIds.contains)
      if valid(game)
    } yield gameRepo initialFen game flatMap { initialFen =>
      userRepo.usernamesByIds(game.userIds) map { usernames =>
        def username(color: chess.Color) =
          game.player(color).userId flatMap { id =>
            usernames.find(_.toLowerCase == id)
          } orElse game.player(color).userId getOrElse "?"
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
            "fen"    -> initialFen.map(_.value),
            "moves"  -> game.pgnMoves.mkString(" ")
          )
          .some
      }
    })

  private val logger = lila.log("explorer")
}
