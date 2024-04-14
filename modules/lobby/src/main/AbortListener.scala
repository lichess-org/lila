package lila.lobby

import lila.core.game.Source

final private class AbortListener(
    userApi: lila.core.user.UserApi,
    gameRepo: lila.core.game.GameRepo,
    seekApi: SeekApi,
    lobbyActor: LobbySyncActor,
    fixedColor: scalalib.cache.ExpireSetMemo[GameId]
)(using Executor):

  def apply(pov: Pov): Funit =
    (pov.game.isCorrespondence
      .so(recreateSeek(pov)))
      .andDo(cancelColorIncrement(pov))
      .andDo(lobbyActor.registerAbortedGame(pov.game))

  private def cancelColorIncrement(pov: Pov): Unit =
    if pov.game.source.exists: s =>
        s == Source.Lobby || s == Source.Pool && !fixedColor.get(pov.game.id)
    then
      pov.game.userIds match
        case List(u1, u2) =>
          userApi.incColor(u1, -1)
          userApi.incColor(u2, 1)
        case _ =>

  private def recreateSeek(pov: Pov): Funit =
    pov.player.userId.so: aborterId =>
      seekApi.findArchived(pov.gameId).flatMapz { seek =>
        (seek.user.id != aborterId).so:
          worthRecreating(seek).flatMapz:
            seekApi.insert(Seek.renew(seek))
      }

  private def worthRecreating(seek: Seek): Fu[Boolean] =
    userApi.byId(seek.user.id).map {
      _.exists: u =>
        u.enabled.yes && !u.lame
    }
