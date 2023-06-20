package lila.round

import lila.user.UserRepo
import lila.game.{ Game, Pov, GameRepo }
import lila.game.JsonView.given
import lila.round.actorApi.SocketStatus
import play.api.libs.json.{ Json, JsObject }
import lila.common.{ Bus, Preload, ApiVersion }
import lila.socket.Socket
import lila.common.LightUser
import lila.common.Json.given
import LightUser.lightUserWrites
import chess.Color
import chess.ByColor

final private class RoundMobileSocket(
    lightUserGet: LightUser.Getter,
    gameRepo: GameRepo,
    jsonView: lila.game.JsonView,
    roundJson: JsonView
)(using Executor):

  private given play.api.i18n.Lang = lila.i18n.defaultLang

  def json(game: Game, socket: SocketStatus, id: GameAnyId): Fu[JsObject] = for
    initialFen <- gameRepo.initialFen(game)
    whiteUser  <- game.whitePlayer.userId.so(lightUserGet)
    blackUser  <- game.blackPlayer.userId.so(lightUserGet)
    users = ByColor(whiteUser, blackUser)
  yield
    def playerJson(color: Color) =
      val player = game player color
      jsonView
        .player(player, users(color))
        .add("isGone" -> (!player.isAi && socket.isGone(player.color)))
        .add("onGame" -> (player.isAi || socket.onGame(player.color)))
    Json
      .obj(
        "game" -> {
          jsonView.base(game, initialFen) ++ Json.obj(
            "pgn" -> game.sans.mkString(" ")
          )
        },
        "white"  -> playerJson(Color.White),
        "black"  -> playerJson(Color.Black),
        "socket" -> socket.version
      )
      .add("clock", game.clock.map(roundJson.clockJson))
      .add("correspondence", game.correspondenceClock)
      .add("youAre", id.playerId.flatMap(game.player(_)).map(_.color))
