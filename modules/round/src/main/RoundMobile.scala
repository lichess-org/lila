package lila.round

import lila.user.{ UserRepo, Me }
import lila.game.{ Game, Pov, GameRepo }
import lila.game.JsonView.given
import lila.round.actorApi.{ SocketStatus, GameAndSocketStatus }
import play.api.libs.json.{ Json, JsObject, JsArray }
import lila.common.{ Bus, Preload, ApiVersion, LightUser }
import lila.socket.Socket
import lila.common.Json.given
import chess.{ Color, ByColor }
import lila.pref.Pref
import lila.chat.Chat

final private class RoundMobile(
    lightUserGet: LightUser.Getter,
    gameRepo: GameRepo,
    jsonView: lila.game.JsonView,
    roundJson: JsonView,
    prefApi: lila.pref.PrefApi,
    takebacker: Takebacker,
    moretimer: Moretimer,
    chatApi: lila.chat.ChatApi
)(using Executor, lila.user.FlairApi):

  private given play.api.i18n.Lang = lila.i18n.defaultLang

  def json(gameSockets: List[GameAndSocketStatus])(using me: Me): Fu[JsArray] =
    gameSockets
      .flatMap: gs =>
        Pov(gs.game, me).map(_ -> gs.socket)
      .traverse: (pov, socket) =>
        json(pov.game, pov.fullId.anyId, socket.some)
      .map(JsArray(_))

  def json(game: Game, id: GameAnyId, socket: Option[SocketStatus]): Fu[JsObject] = for
    initialFen <- gameRepo.initialFen(game)
    myPlayer = id.playerId.flatMap(game.player(_))
    users        <- game.userIdPair.traverse(_ so lightUserGet)
    prefs        <- prefApi.byId(game.userIdPair)
    takebackable <- takebacker.isAllowedIn(game, Preload(prefs))
    moretimeable <- moretimer.isAllowedIn(game, Preload(prefs))
    chat         <- getPlayerChat(game, myPlayer.exists(_.hasUser))
    chatLines    <- chat.map(_.chat) soFu lila.chat.JsonView.asyncLines
  yield
    def playerJson(color: Color) =
      val player = game player color
      jsonView
        .player(player, users(color))
        .add("isGone" -> (game.forceDrawable && socket.exists(_.isGone(player.color))))
        .add("onGame" -> (player.isAi || socket.exists(_.onGame(player.color))))
    Json
      .obj(
        "game" -> {
          jsonView.base(game, initialFen) ++ Json
            .obj("pgn" -> game.sans.mkString(" "))
            .add("drawOffers" -> (!game.drawOffers.isEmpty).option(game.drawOffers.normalizedPlies))
        },
        "white"  -> playerJson(Color.White),
        "black"  -> playerJson(Color.Black),
        "socket" -> socket.so(_.version).value
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
      .add("prefs", myPlayer.map(p => prefs(p.color)).map(prefsJson(game, _)))
      .add(
        "chat",
        chat.map: c =>
          Json
            .obj("lines" -> chatLines)
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
      for
        chat  <- chatApi.playerChat.findIf(game.id into ChatId, !game.justCreated)
        lines <- lila.chat.JsonView.asyncLines(chat)
      yield Chat.Restricted(chat, lines, restricted = game.fromLobby && !isAuth).some
