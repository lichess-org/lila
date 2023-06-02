package lila.round

import lila.user.UserRepo
import lila.game.{ Game, Pov, GameRepo }
import lila.round.actorApi.SocketStatus
import play.api.libs.json.JsObject
import lila.common.{ Bus, Preload, ApiVersion }
import lila.socket.Socket

final private class RoundMobileSocket(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    jsonView: JsonView
)(using Executor):
  private given play.api.i18n.Lang = lila.i18n.defaultLang

  def watcher(pov: Pov, socket: SocketStatus): Fu[JsObject] =
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
