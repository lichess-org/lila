package lila.fide

import reactivemongo.api.bson.*
import chess.{ ByColor, FideId }

import lila.db.dsl.{ given, * }

final class FidePlayerApi(private[fide] val coll: Coll, cacheApi: lila.memo.CacheApi)(using Executor):

  import FidePlayerApi.playerHandler
  import FidePlayer.*

  def fetch(id: FideId): Fu[Option[FidePlayer]] = coll.byId[FidePlayer](id)

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
    _.expireAfterWrite(1.minute).buildAsyncFuture: id =>
      coll.byId[FidePlayer](id)

  private val guessPlayerCache =
    cacheApi[TokenTitle, Option[FidePlayer]](1024, "player.fidePlayer.byName"):
      _.expireAfterWrite(1.minute).buildAsyncFuture: tt =>
        coll.find($doc("token" -> tt.token, "title" -> tt.title)).cursor[FidePlayer]().list(2) map:
          case List(onlyMatch) => onlyMatch.some
          case _               => none

private object FidePlayerApi:

  given playerHandler: BSONDocumentHandler[FidePlayer] = Macros.handler
