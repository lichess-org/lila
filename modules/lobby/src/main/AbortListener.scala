package lila.lobby

import lila.core.game.*

final private class AbortListener(
    userApi: lila.core.user.UserApi,
    gameRepo: lila.core.game.GameRepo,
    seekApi: SeekApi,
    lobbyActor: LobbySyncActor,
    fixedColor: scalalib.cache.ExpireSetMemo[GameId]
)(using Executor):

  lila.common.Bus.subscribeFun("abortGame"):
    case AbortedBy(pov) => onAbort(pov)

  lila.common.Bus.subscribeFun("finishGame"):
    // this includes aborted games too
    case FinishGame(game, _) if game.hasFewerMovesThanExpected => onEarlyFinish(game)

  private def filter(g: Game) = g.source.exists: s =>
    s == Source.Lobby || s == Source.Pool

  private def onAbort(pov: Pov): Funit =
    filter(pov.game).so:
      lobbyActor.registerAbortedGame(pov.game)
      pov.game.isCorrespondence.so(recreateSeek(pov))

  private def onEarlyFinish(game: Game): Unit =
    if filter(game)
    then cancelBothColorIncrements(game)

  private def cancelBothColorIncrements(game: Game): Unit =
    if !fixedColor.get(game.id)
    then
      game.userIds match
        case List(u1, u2) =>
          userApi.incColor(u1, Color.black)
          userApi.incColor(u2, Color.white)
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
