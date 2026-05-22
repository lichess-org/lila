package lila.round

import chess.Color
import play.api.libs.json.{ JsArray, JsObject, Json }

import lila.chat.Chat
import lila.common.Json.given
import scalalib.data.Preload
import lila.pref.Pref
import lila.round.RoundGame.*
import lila.round.Forecast.given
import lila.core.LightUser

object RoundMobile:

  enum UseCase(
      val socketStatus: Option[SocketStatus],
      val chat: Boolean,
      val prefs: Boolean,
      val bookmark: Boolean,
      val forecast: Boolean,
      val as: Option[Option[MyId]]
  ):
    // full round for every-day use
    case Online(socket: SocketStatus)(using me: Option[MyId])
        extends UseCase(
          socket.some,
          chat = true,
          prefs = true,
          bookmark = true,
          forecast = true,
          as = me.some
        )
    // correspondence game sent through firebase data
    // https://github.com/lichess-org/mobile/blob/main/lib/src/model/correspondence/offline_correspondence_game.dart
    case Offline
        extends UseCase(none, chat = false, prefs = false, bookmark = false, forecast = false, as = none)
    // requested by the forecast analysis board to refresh the game
    case Forecast
        extends UseCase(none, chat = false, prefs = false, bookmark = false, forecast = true, as = none)

final class RoundMobile(
    lightUserGet: LightUser.Getter,
    userApi: lila.core.user.UserApi,
    gameRepo: lila.core.game.GameRepo,
    jsonView: lila.game.JsonView,
    roundJson: JsonView,
    prefApi: lila.pref.PrefApi,
    takebacker: Takebacker,
    moretimer: Moretimer,
    forecastApi: ForecastApi,
    isOfferingRematch: lila.core.round.IsOfferingRematch,
    chatApi: lila.chat.ChatApi,
    chatJson: lila.chat.ChatJsonView,
    bookmarkExists: lila.core.misc.BookmarkExists,
    tourApi: lila.core.data.CircularDep[lila.core.tournament.TournamentApi]
)(using Executor):

  import RoundMobile.*

  def online(gameSockets: List[GameAndSocketStatus])(using me: Me): Fu[JsArray] =
    gameSockets
      .flatMap: gs =>
        Pov(gs.game, me).map(_ -> gs.socket)
      .parallel: (pov, socket) =>
        online(pov.game, pov.fullId.anyId, socket)
      .map(JsArray(_))

  def online(game: Game, id: GameAnyId, socket: SocketStatus)(using Option[MyId]): Fu[JsObject] =
    forUseCase(game, id, UseCase.Online(socket))

  def offline(game: Game, id: GameAnyId): Fu[JsObject] =
    forUseCase(game, id, UseCase.Offline)

  def forecast(game: Game, id: GameAnyId)(using Me): Fu[JsObject] =
    forUseCase(game, id, UseCase.Forecast)

  private def forUseCase(game: Game, id: GameAnyId, use: UseCase): Fu[JsObject] =
    for
      initialFen <- gameRepo.initialFen(game)
      myPlayer = id.playerId
        .flatMap(game.playerById)
        .filter: player =>
          use.as.forall: me =>
            player.userId.fold(me.isEmpty)(_.is(me))
      users <- game.userIdPair.traverse(_.so(lightUserGet))
      prefs <- prefApi.byId(game.userIdPair)
      takebackable <- takebacker.isAllowedIn(game, Preload(prefs))
      moretimeable <- moretimer.isAllowedIn(game, Preload(prefs), force = false)
      chat <- use.chat.so:
        if myPlayer.isDefined
        then getPlayerChat(game, myPlayer.exists(_.hasUser))
        else getWatcherChat(game)(using use.as.flatten)
      bookmarked <- use.bookmark.so(bookmarkExists(game, myPlayer.flatMap(_.userId)))
      forecast <- use.forecast.so(myPlayer).so(p => forecastApi.loadForDisplay(Pov(game, p)))
      tournament <- tourInfo(game)
    yield
      def playerJson(color: Color) =
        val pov = Pov(game, color)
        jsonView
          .player(pov.player, users(color))
          .add("isGone" -> (game.forceDrawable && use.socketStatus.exists(_.isGone(pov.color))))
          .add(
            "onGame" -> (myPlayer.map(_.color).has(color) ||
              pov.player.isAi ||
              use.socketStatus.exists(_.onGame(pov.color)))
          )
          .add("offeringRematch" -> isOfferingRematch.exec(pov.ref))
          .add("offeringDraw" -> pov.player.isOfferingDraw)
          .add("proposingTakeback" -> pov.player.isProposingTakeback)
          .add("berserk" -> pov.player.berserk)
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
        .add("expiration" -> lila.game.JsonView.expiration(game))
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
              .obj("lines" -> c.lines)
              .add("restricted", c.restricted)
        )
        .add("bookmarked", bookmarked)
        .add("tournament", tournament)
        .add("forecast" -> forecast)

  private def tourInfo(game: Game): Fu[Option[JsObject]] =
    game.tournamentId
      .so(tourApi.resolve().getCached)
      .flatMapz: tour =>
        tourApi
          .resolve()
          .getGameRanks(tour, game)
          .map: ranks =>
            Json
              .obj(
                "id" -> tour.id,
                "name" -> tour.name,
                "secondsLeft" -> tour.secondsToFinish
              )
              .add("berserkable" -> tour.isStarted.option(tour.berserkable))
              .add("ranks" -> ranks)
          .dmap(some)

  private def prefsJson(game: Game, pref: Pref): JsObject = Json
    .obj(
      "autoQueen" ->
        (if game.variant == chess.variant.Antichess then Pref.AutoQueen.NEVER else pref.autoQueen),
      "zen" -> pref.zen
    )
    .add("confirmResign", pref.confirmResign == Pref.ConfirmResign.YES)
    .add("enablePremove", pref.premove)
    .add("submitMove", roundJson.submitMovePref(pref, game, nvui = false))

  private def getPlayerChat(game: Game, isAuth: Boolean): Fu[Option[Chat.RestrictedLines]] =
    game.hasChat.so:
      for
        chat <- chatApi.playerChat.findIf(game.id.into(ChatId), !game.justCreated)
        filtered = chat.copy(lines = chat.lines.filterNot(l => l.troll || l.deleted))
        lines <- chatJson.asyncLines(filtered)
      yield Chat.RestrictedLines(lines, restricted = game.sourceIs(_.Lobby) && !isAuth).some

  given lila.chat.AllMessages = lila.chat.AllMessages.No

  private def getWatcherChat(game: Game)(using myId: Option[MyId]): Fu[Option[Chat.RestrictedLines]] =
    game.hasChat.so:
      for
        me <- myId.so(userApi.me)
        chat <- chatApi.userChat.findMine(ChatId(s"${game.id}/w"), !game.justCreated)(using me)
      yield Chat.RestrictedLines(chat.lines, restricted = me.isEmpty).some
