package lila.fide

import reactivemongo.api.bson.*
import chess.{ ByColor, FideId }

import lila.db.dsl.{ given, * }

final class FidePlayerApi(repo: FideRepo, cacheApi: lila.memo.CacheApi)(using Executor):

  import repo.player.handler
  import FidePlayer.*

  def players(ids: ByColor[Option[FideId]]): Fu[ByColor[Option[FidePlayer]]] =
    ids.traverse:
      _.so(idToPlayerCache.get)

  def federationsOf(ids: List[FideId]): Fu[Federation.ByFideIds] =
    idToPlayerCache.getAll(ids) map:
      _.mapValues(_.flatMap(_.fed))
        .collect:
          case (k, Some(v)) => k -> v
        .toMap

  def guessPlayer(
      fideId: Option[FideId],
      name: Option[PlayerName],
      title: Option[UserTitle]
  ): Fu[Option[FidePlayer]] = fideId match
    case Some(fideId) => idToPlayerCache.get(fideId)
    case None         => name.map(FidePlayer.tokenize).map(TokenTitle(_, title)).so(guessPlayerCache.get)

  private val idToPlayerCache = cacheApi[FideId, Option[FidePlayer]](1024, "player.fidePlayer.byId"):
    _.expireAfterWrite(1.minute).buildAsyncFuture(repo.player.fetch)

  private val guessPlayerCache =
    cacheApi[TokenTitle, Option[FidePlayer]](1024, "player.fidePlayer.byName"):
      _.expireAfterWrite(1.minute).buildAsyncFuture: tt =>
        repo.playerColl
          .find($doc("token" -> tt.token, "title" -> tt.title))
          .cursor[FidePlayer]()
          .list(2) map:
          case List(onlyMatch) => onlyMatch.some
          case _               => none
