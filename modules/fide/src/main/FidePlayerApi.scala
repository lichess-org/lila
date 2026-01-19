package lila.fide

import chess.{ ByColor, FideId, PlayerName, PlayerTitle }
import reactivemongo.api.bson.*

import lila.core.fide.Federation
import lila.db.dsl.{ *, given }
import lila.memo.{ CacheApi, PicfitApi }
import lila.memo.PicfitImage

final class FidePlayerApi(repo: FideRepo, cacheApi: CacheApi, picfitApi: PicfitApi)(using Executor):

  import repo.player.handler

  export repo.player.{ fetch, setPhotoCredit }

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

  def withFollow(id: FideId)(using me: Option[Me]): Fu[Option[FidePlayer.WithFollow]] =
    idToPlayerCache
      .get(id)
      .flatMapz: player =>
        me.map(_.userId)
          .so(repo.follower.isFollowing(_, id))
          .map(FidePlayer.WithFollow(player, _).some)

  def uploadPhoto(p: FidePlayer, photo: PicfitApi.FilePart)(using me: Me): Fu[PicfitImage] =
    for
      pic <- picfitApi.uploadFile(photo, me.userId, s"fidePlayer:${p.id}".some, requestAutomod = false)
      _ <- repo.player.setPhoto(p.id, FidePlayer.PlayerPhoto(pic.id, none))
    yield pic

  private val idToPlayerCache = cacheApi[FideId, Option[FidePlayer]](8_192, "player.fidePlayer.byId"):
    _.expireAfterWrite(3.minutes).buildAsyncFuture(repo.player.fetch)

  export idToPlayerCache.get

  def photos(ids: Set[FideId]): Fu[Map[FideId, FidePlayer.PlayerPhoto]] =
    ids.toList
      .traverse(get)
      .map:
        _.flatten.flatMap: p =>
          p.photo.map(p.id -> _)
      .map(_.toMap)

  object guessPlayer:

    private case class TitleName(title: Option[PlayerTitle], name: PlayerName)

    def apply(
        fideId: Option[FideId],
        name: Option[PlayerName],
        title: Option[PlayerTitle]
    ): Fu[Option[FidePlayer]] = fideId match
      case Some(fideId) => idToPlayerCache.get(fideId)
      case None => name.map(TitleName(title, _)).so(cache.get)

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
                case _ => none
