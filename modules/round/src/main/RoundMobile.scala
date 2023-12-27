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

object RoundMobile:

  enum UseCase(val socketStatus: Option[SocketStatus], val chat: Boolean, val prefs: Boolean):
    // full round for every-day use
    case Online(socket: SocketStatus) extends UseCase(socket.some, chat = true, prefs = true)
    // correspondence game sent through firebase data
    // https://github.com/lichess-org/mobile/blob/main/lib/src/model/correspondence/offline_correspondence_game.dart
    case Offline extends UseCase(none, chat = false, prefs = false)

final class RoundMobile(
    lightUserGet: LightUser.Getter,
    gameRepo: GameRepo,
    jsonView: lila.game.JsonView,
    roundJson: JsonView,
    prefApi: lila.pref.PrefApi,
    takebacker: Takebacker,
    moretimer: Moretimer,
    isOfferingRematch: IsOfferingRematch,
    chatApi: lila.chat.ChatApi
)(using Executor, lila.user.FlairApi):

  import RoundMobile.*
  private given play.api.i18n.Lang = lila.i18n.defaultLang

  def online(gameSockets: List[GameAndSocketStatus])(using me: Me): Fu[JsArray] =
    gameSockets
      .flatMap: gs =>
        Pov(gs.game, me).map(_ -> gs.socket)
      .traverse: (pov, socket) =>
        online(pov.game, pov.fullId.anyId, socket)
      .map(JsArray(_))

  def online(game: Game, id: GameAnyId, socket: SocketStatus): Fu[JsObject] =
    forUseCase(game, id, UseCase.Online(socket))

  def offline(game: Game, id: GameAnyId): Fu[JsObject] =
    forUseCase(game, id, UseCase.Offline)

  private def forUseCase(game: Game, id: GameAnyId, use: UseCase): Fu[JsObject] =
    for
      initialFen <- gameRepo.initialFen(game)
      myPlayer = id.playerId.flatMap(game.player(_))
      users        <- game.userIdPair.traverse(_ so lightUserGet)
      prefs        <- prefApi.byId(game.userIdPair)
      takebackable <- takebacker.isAllowedIn(game, Preload(prefs))
      moretimeable <- moretimer.isAllowedIn(game, Preload(prefs))
      chat         <- use.chat so getPlayerChat(game, myPlayer.exists(_.hasUser))
      chatLines    <- chat.map(_.chat) soFu lila.chat.JsonView.asyncLines
    yield
      def playerJson(color: Color) =
        val pov = Pov(game, color)
        jsonView
          .player(pov.player, users(color))
          .add("isGone" -> (game.forceDrawable && use.socketStatus.exists(_.isGone(pov.color))))
          .add("onGame" -> (pov.player.isAi || use.socketStatus.exists(_.onGame(pov.color))))
          .add("offeringRematch" -> isOfferingRematch(pov))
          .add("offeringDraw" -> pov.player.isOfferingDraw)
          .add("proposingTakeback" -> pov.player.isProposingTakeback)
      Json
        .obj(
          "game" -> {
            jsonView.base(game, initialFen) ++ Json
              .obj("pgn" -> game.sans.mkString(" "))
              .add("drawOffers" -> (!game.drawOffers.isEmpty).option(game.drawOffers.normalizedPlies))
          },
          "white" -> playerJson(Color.White),
          "black" -> playerJson(Color.Black)
        )
        .add("socket" -> use.socketStatus.map(_.version))
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
        .add("prefs", myPlayer.flatMap(p => prefs.map(_(p.color))).map(prefsJson(game, _)))
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
