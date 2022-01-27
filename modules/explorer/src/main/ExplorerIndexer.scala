package lila.explorer

import akka.stream.scaladsl._
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import play.api.libs.ws.JsonBodyWritables._
import lila.common.ThreadLocalRandom.nextFloat
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

  // probability of the game being indexed, between 0 and 1
  private def probability(game: Game, rating: Int): Float = {
    import lila.rating.PerfType._
    game.perfType ?? {
      case Correspondence | Classical => 1.00f

      case Rapid if rating >= 2200 => 1.00f
      case Rapid if rating >= 2000 => 0.83f
      case Rapid if rating >= 1800 => 0.46f
      case Rapid if rating >= 1600 => 0.39f
      case Rapid                   => 0.02f

      case Blitz if rating >= 2500 => 1.00f
      case Blitz if rating >= 2200 => 0.38f
      case Blitz if rating >= 2000 => 0.18f
      case Blitz if rating >= 1600 => 0.13f
      case Blitz                   => 0.02f

      case Bullet if rating >= 2500 => 1.00f
      case Bullet if rating >= 2200 => 0.48f
      case Bullet if rating >= 2000 => 0.27f
      case Bullet if rating >= 1800 => 0.19f
      case Bullet if rating >= 1600 => 0.18f
      case Bullet                   => 0.02f

      case UltraBullet => 1.00f

      case _ if rating >= 1600 => 1.00f // variant games
      case _                   => 0.50f // noob variant games
    }
  }

  private def makeJson(game: Game, botUserIds: Set[User.ID]): Fu[Option[JsObject]] =
    ~(for {
      whiteRating <- stableRating(game.whitePlayer)
      blackRating <- stableRating(game.blackPlayer)
      if whiteRating >= 1501
      if blackRating >= 1501
      averageRating = (whiteRating + blackRating) / 2
      if probability(game, averageRating) > nextFloat()
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
