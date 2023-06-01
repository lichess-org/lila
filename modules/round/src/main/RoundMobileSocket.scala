package lila.round

import lila.user.UserRepo
import lila.game.{ Game, Pov, GameRepo }
import lila.round.actorApi.SocketStatus
import play.api.libs.json.JsObject
import lila.common.{ Bus, Preload, ApiVersion }
import lila.socket.Socket
import lila.hub.actorApi.socket.remote.TellSriOut

final private class RoundMobileSocket(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    jsonView: JsonView
)(using Executor):
  private given play.api.i18n.Lang = lila.i18n.defaultLang

  def watcher(sri: Socket.Sri, pov: Pov, socket: SocketStatus): Funit =
    gameRepo
      .initialFen(pov.game)
      .flatMap: initialFen =>
        jsonView.watcherJson(
          pov,
          pref = none,
          ApiVersion.mobile,
          tv = none,
          initialFen = initialFen,
          flags = JsonView.WithFlags(),
          socketStatus = Preload(socket)
        )
      .map: json =>
        Bus.publish(TellSriOut(sri.value, json), "remoteSocketOut")
