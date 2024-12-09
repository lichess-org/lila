package lila.fide

import chess.{ ByColor, FideId, PlayerName, PlayerTitle }
import reactivemongo.api.bson.*

import lila.core.fide.Federation
import lila.db.dsl.{ *, given }

final class FidePlayerApi(repo: FideRepo, cacheApi: lila.memo.CacheApi)(using Executor):

  import repo.player.handler
  import FidePlayer.*

  export repo.player.fetch

  def players(ids: ByColor[Option[FideId]]): Fu[ByColor[Option[FidePlayer]]] =
    ids.traverse:
      _.so(idToPlayerCache.get)

  def federationsOf(ids: List[FideId]): Fu[Federation.ByFideIds] = ids.nonEmpty.so:
    idToPlayerCache
      .getAll(ids)
      .map:
        _.view
          .mapValues(_.flatMap(_.fed))
          .collect:
            case (k, Some(v)) => k -> v
          .toMap

  def federationNamesOf(ids: List[FideId]): Fu[Map[Federation.Id, Federation.Name]] =
    idToPlayerCache
      .getAll(ids)
      .map: players =>
        lila.fide.Federation.namesByIds(players.values.flatMap(_.flatMap(_.fed)))

  private val idToPlayerCache = cacheApi[FideId, Option[FidePlayer]](4096, "player.fidePlayer.byId"):
    _.expireAfterWrite(3.minutes).buildAsyncFuture(repo.player.fetch)

  export idToPlayerCache.get

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
        _.expireAfterWrite(5.minutes).buildAsyncFuture: p =>
          val token = FidePlayer.tokenize(p.name.value)
          (token.sizeIs > 2).so:
            repo.playerColl
              .find($doc("token" -> token, "title" -> p.title))
              .cursor[FidePlayer]()
              .list(2)
              .map:
                case List(onlyMatch) => onlyMatch.some
                case _               => none
