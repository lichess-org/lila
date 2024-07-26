package lila.round

import chess.Color
import play.api.libs.json.{ JsArray, JsObject, Json }

import lila.chat.Chat
import lila.common.Json.given
import lila.core.LightUser
import lila.core.data.Preload
import lila.pref.Pref
import lila.round.RoundGame.*

object RoundMobile:

  enum UseCase(
      val socketStatus: Option[SocketStatus],
      val chat: Boolean,
      val prefs: Boolean,
      val bookmark: Boolean
  ):
    // full round for every-day use
    case Online(socket: SocketStatus) extends UseCase(socket.some, chat = true, prefs = true, bookmark = true)
    // correspondence game sent through firebase data
    // https://github.com/lichess-org/mobile/blob/main/lib/src/model/correspondence/offline_correspondence_game.dart
    case Offline extends UseCase(none, chat = false, prefs = false, bookmark = false)

final class RoundMobile(
    lightUserGet: LightUser.Getter,
    gameRepo: lila.core.game.GameRepo,
    jsonView: lila.game.JsonView,
    roundJson: JsonView,
    prefApi: lila.pref.PrefApi,
    takebacker: Takebacker,
    moretimer: Moretimer,
    isOfferingRematch: lila.core.round.IsOfferingRematch,
    chatApi: lila.chat.ChatApi,
    bookmarkExists: lila.core.bookmark.BookmarkExists
)(using Executor, lila.core.user.FlairGetMap):

  import RoundMobile.*
  private given play.api.i18n.Lang = lila.core.i18n.defaultLang

  def online(gameSockets: List[GameAndSocketStatus])(using me: Me): Fu[JsArray] =
    gameSockets
      .flatMap: gs =>
        Pov(gs.game, me).map(_ -> gs.socket)
      .parallel: (pov, socket) =>
        online(pov.game, pov.fullId.anyId, socket)
      .map(JsArray(_))

  def online(game: Game, id: GameAnyId, socket: SocketStatus): Fu[JsObject] =
    forUseCase(game, id, UseCase.Online(socket))

  def offline(game: Game, id: GameAnyId): Fu[JsObject] =
    forUseCase(game, id, UseCase.Offline)

  private def forUseCase(game: Game, id: GameAnyId, use: UseCase): Fu[JsObject] =
    for
      initialFen <- gameRepo.initialFen(game)
      myPlayer = id.playerId.flatMap(game.playerById)
      users        <- game.userIdPair.traverse(_.so(lightUserGet))
      prefs        <- prefApi.byId(game.userIdPair)
      takebackable <- takebacker.isAllowedIn(game, Preload(prefs))
      moretimeable <- moretimer.isAllowedIn(game, Preload(prefs))
      chat         <- use.chat.so(getPlayerChat(game, myPlayer.exists(_.hasUser)))
      chatLines    <- chat.map(_.chat).soFu(lila.chat.JsonView.asyncLines)
      bookmarked   <- use.bookmark.so(bookmarkExists(game, myPlayer.flatMap(_.userId)))
    yield
      def playerJson(color: Color) =
        val pov = Pov(game, color)
        jsonView
          .player(pov.player, users(color))
          .add("isGone" -> (game.forceDrawable && use.socketStatus.exists(_.isGone(pov.color))))
          .add("onGame" -> (pov.player.isAi || use.socketStatus.exists(_.onGame(pov.color))))
          .add("offeringRematch" -> isOfferingRematch(pov.ref))
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
        .add("prefs", use.prefs.so(myPlayer.map(p => prefs(p.color)).map(prefsJson(game, _))))
        .add(
          "chat",
          chat.map: c =>
            Json
              .obj("lines" -> chatLines)
              .add("restricted", c.restricted)
        )
        .add("bookmarked", bookmarked)

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
        chat  <- chatApi.playerChat.findIf(game.id.into(ChatId), game.secondsSinceCreation > 1)
        lines <- lila.chat.JsonView.asyncLines(chat)
      yield Chat.Restricted(chat, lines, restricted = game.sourceIs(_.Lobby) && !isAuth).some
