package lila.api

import shogi.format.{ Notation, Tag, Tags }
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.user.User

final class RealPlayerApi(
    cacheApi: lila.memo.CacheApi,
    ws: WSClient
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(url: String): Fu[Option[RealPlayers]] = cache get url

  private val cache = cacheApi[String, Option[RealPlayers]](4, "api.realPlayer") {
    _.expireAfterAccess(10 seconds)
      .buildAsyncFuture { url =>
        ws.url(url)
          .withRequestTimeout(3.seconds)
          .get()
          .map { res =>
            val valid =
              res.status == 200 &&
                res.headers
                  .get("Content-Type")
                  .exists(_.exists(_ startsWith "text/plain"))
            valid ?? {
              res.body.linesIterator
                .take(9999)
                .toList
                .flatMap { line =>
                  line.split(';').map(_.trim) match {
                    case Array(id, name, rating) =>
                      Some(id -> RealPlayer(name.some.filter(_.nonEmpty), rating.toIntOption))
                    case Array(id, name) => Some(id -> RealPlayer(name.some.filter(_.nonEmpty), none))
                    case _               => none
                  }
                }
                .toMap
                .some
                .map(RealPlayers)
            }
          }
          .nevermind
      }
  }
}

case class RealPlayers(players: Map[User.ID, RealPlayer]) {

  def update(game: lila.game.Game, notation: Notation) =
    notation withTags (
      notation.tags ++ Tags {
        game.players.flatMap { player =>
          player.userId.flatMap(players.get) ?? { rp =>
            List(
              rp.name.map { name => Tag(player.color.fold(Tag.Sente, Tag.Gote), name) },
              rp.rating.map { rating => Tag(player.color.fold(Tag.SenteElo, Tag.GoteElo), rating.toString) }
            ).flatten
          }
        }
      }
    )
}

case class RealPlayer(name: Option[String], rating: Option[Int])
