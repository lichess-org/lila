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
import chess.{ Color, ByColor }
import lila.pref.Pref
import lila.chat.Chat

final private class RoundMobileSocket(
    lightUserGet: LightUser.Getter,
    gameRepo: GameRepo,
    jsonView: lila.game.JsonView,
    roundJson: JsonView,
    prefApi: lila.pref.PrefApi,
    takebacker: Takebacker,
    moretimer: Moretimer,
    chatApi: lila.chat.ChatApi
)(using Executor):

  private given play.api.i18n.Lang = lila.i18n.defaultLang

  def json(game: Game, socket: SocketStatus, id: GameAnyId): Fu[JsObject] = for
    initialFen <- gameRepo.initialFen(game)
    whiteUser  <- game.whitePlayer.userId.so(lightUserGet)
    blackUser  <- game.blackPlayer.userId.so(lightUserGet)
    users    = ByColor(whiteUser, blackUser)
    myPlayer = id.playerId.flatMap(game.player(_))
    prefs        <- myPlayer.flatMap(_.userId).so(prefApi.getPrefById)
    takebackable <- takebacker.isAllowedIn(game)
    moretimeable <- moretimer.isAllowedIn(game)
    chat         <- getPlayerChat(game, myPlayer.exists(_.hasUser))
  yield
    def playerJson(color: Color) =
      val player = game player color
      jsonView
        .player(player, users(color))
        .add("isGone" -> (game.forceDrawable && socket.isGone(player.color)))
        .add("onGame" -> (player.isAi || socket.onGame(player.color)))
    Json
      .obj(
        "game" -> {
          jsonView.base(game, initialFen) ++ Json
            .obj("pgn" -> game.sans.mkString(" "))
            .add("drawOffers" -> (!game.drawOffers.isEmpty).option(game.drawOffers.normalizedPlies))
        },
        "white"  -> playerJson(Color.White),
        "black"  -> playerJson(Color.Black),
        "socket" -> socket.version
      )
      .add("expiration" -> game.expirable.option:
        Json.obj(
          "idleMillis"   -> (nowMillis - game.movedAt.toMillis),
          "millisToMove" -> game.timeForFirstMove.millis
        )
      )
      .add("clock", game.clock.map(roundJson.clockJson))
      .add("correspondence", game.correspondenceClock)
      .add("takebackable" -> takebackable)
      .add("moretimeable" -> moretimeable)
      .add("youAre", myPlayer.map(_.color))
      .add("prefs", prefs.map(prefsJson(game, _)))
      .add(
        "chat",
        chat.map: c =>
          Json
            .obj("lines" -> lila.chat.JsonView(c.chat))
            .add("restricted", c.restricted)
      )

  private def prefsJson(game: Game, pref: Pref): JsObject = Json
    .obj(
      "autoQueen" ->
        (if game.variant == chess.variant.Antichess then Pref.AutoQueen.NEVER else pref.autoQueen),
      "zen" -> pref.zen
    )
    .add("confirmResign", pref.confirmResign == Pref.ConfirmResign.YES)
    .add("enablePremove", pref.premove)
    .add("submitMove", roundJson.submitMovePref(pref, game, nvui = false))

  private def getPlayerChat(game: Game, isAuth: Boolean): Fu[Option[Chat.Restricted]] =
    game.hasChat.so:
      chatApi.playerChat.findIf(game.id into ChatId, !game.justCreated) map { chat =>
        Chat.Restricted(chat, restricted = game.fromLobby && !isAuth).some
      }
