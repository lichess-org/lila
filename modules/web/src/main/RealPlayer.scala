package lila.web

import chess.format.pgn.{ Pgn, Tag, Tags }
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

final class RealPlayerApi(
    cacheApi: lila.memo.CacheApi,
    ws: StandaloneWSClient
)(using Executor):

  def apply(url: String): Fu[Option[RealPlayers]] = cache.get(url)

  private val cache = cacheApi[String, Option[RealPlayers]](4, "api.realPlayer"):
    _.expireAfterAccess(30 seconds).buildAsyncFuture: url =>
      ws.url(url)
        .withRequestTimeout(3.seconds)
        .get()
        .map: res =>
          val valid =
            res.status == 200 &&
              res.headers
                .get("Content-Type")
                .exists(_.exists(_.startsWith("text/plain")))
          valid.so:
            res
              .body[String]
              .linesIterator
              .take(9999)
              .toList
              .flatMap: line =>
                line.split(';').map(_.trim) match
                  case Array(id, name, rating) => make(id, name.some, rating.some)
                  case Array(id, name)         => make(id, name.some, none)
                  case _                       => none
              .toMap
              .some
              .map(RealPlayers.apply)
        .recoverDefault

  private def make(id: String, name: Option[String], rating: Option[String]) =
    val (n, r) = UserName.from(name.filter(_.nonEmpty)) -> rating.flatMap(_.toIntOption)
    (n.isDefined || r.isDefined).option:
      UserStr(id).id -> RealPlayer(name = n, rating = IntRating.from(r))

case class RealPlayers(players: Map[UserId, RealPlayer]):

  def update(gamePlayers: chess.ByColor[Option[UserId]], pgn: Pgn) =
    pgn.copy(
      tags = pgn.tags ++ Tags:
        gamePlayers
          .mapWithColor: (color, userId) =>
            userId
              .flatMap(players.get)
              .so: rp =>
                List(
                  rp.name.map { name => Tag(_.names(color), name.value) },
                  rp.rating.map { rating => Tag(_.elos(color), rating.toString) }
                ).flatten
          .toList
          .flatten
    )

case class RealPlayer(name: Option[UserName], rating: Option[IntRating])
