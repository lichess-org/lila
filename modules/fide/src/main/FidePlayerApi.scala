package lila.fide

import reactivemongo.api.bson.*
import chess.{ ByColor, FideId, PlayerTitle, PlayerName }

import lila.db.dsl.{ given, * }

final class FidePlayerApi(repo: FideRepo, cacheApi: lila.memo.CacheApi)(using Executor):

  import repo.player.handler
  import FidePlayer.*

  export repo.player.fetch

  def players(ids: ByColor[Option[FideId]]): Fu[ByColor[Option[FidePlayer]]] =
    ids.traverse:
      _.so(idToPlayerCache.get)

  def federationsOf(ids: List[FideId]): Fu[Federation.ByFideIds] =
    idToPlayerCache
      .getAll(ids)
      .map:
        _.view
          .mapValues(_.flatMap(_.fed))
          .collect:
            case (k, Some(v)) => k -> v
          .toMap

  private val idToPlayerCache = cacheApi[FideId, Option[FidePlayer]](1024, "player.fidePlayer.byId"):
    _.expireAfterWrite(3.minutes).buildAsyncFuture(repo.player.fetch)

  def urlToTitle(url: String): Fu[Option[PlayerTitle]] =
    FideWebsite.urlToFideId(url).so(fetch).map(_.flatMap(_.title))

  object guessPlayer:

    private case class TitleName(title: Option[PlayerTitle], name: PlayerName)

    def apply(
        fideId: Option[FideId],
        name: Option[PlayerName],
        title: Option[PlayerTitle]
    ): Fu[Option[FidePlayer]] = fideId match
      case Some(fideId) => idToPlayerCache.get(fideId)
      case None         => name.map(TitleName(title, _)).so(cache.get)

    private val cache =
      cacheApi[TitleName, Option[FidePlayer]](1024, "player.fidePlayer.byName"):
        _.expireAfterWrite(3.minutes).buildAsyncFuture: p =>
          repo.playerColl
            .find($doc("token" -> FidePlayer.tokenize(p.name.value), "title" -> p.title))
            .cursor[FidePlayer]()
            .list(2)
            .map:
              case List(onlyMatch) => onlyMatch.some
              case _               => none
