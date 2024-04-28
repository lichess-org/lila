package controllers

import play.api.http.ContentTypes
import play.api.libs.json.*
import play.api.mvc.Result

import scala.util.chaining.*

import lila.app.{ *, given }
import lila.common.Json.given

import lila.tv.Tv.Channel

final class Tv(env: Env, apiC: => Api, gameC: => Game) extends LilaController(env):

  def index     = Open(serveIndex)
  def indexLang = LangPage(routes.Tv.index)(serveIndex)

  private def serveIndex(using Context) = serveChannel(Channel.Best.key)

  def onChannel(chanKey: String) = Open(serveChannel(chanKey))

  private def serveChannel(chanKey: String)(using Context) =
    Channel.byKey.get(chanKey).so(lichessTv)

  def sides(gameId: GameId, color: String) = Open:
    Found(chess.Color.fromName(color).so { env.round.proxyRepo.pov(gameId, _) }): pov =>
      env.game.crosstableApi.withMatchup(pov.game).flatMap { ct =>
        Ok.page(views.tv.side.sides(pov, ct))
      }

  private given Writes[lila.tv.Tv.Champion] = Json.writes

  def channels = apiC.ApiRequest:
    env.tv.tv.getChampions
      .map {
        _.channels.map { (chan, champ) => chan.key -> champ }
      }
      .map { Json.toJson(_) }
      .dmap(Api.ApiResult.Data.apply)

  private def lichessTv(channel: Channel)(using Context) =
    Found(env.tv.tv.getGameAndHistory(channel)): (game, history) =>
      val flip    = getBool("flip")
      val natural = Pov.naturalOrientation(game)
      val pov     = if flip then !natural else natural
      val onTv    = lila.round.OnTv.Lichess(channel.key, flip)
      env.user.api
        .gamePlayers(game.userIdPair, game.perfKey)
        .flatMap: users =>
          gameC.preloadUsers(users)
          negotiateApi(
            html = for
              tour   <- env.tournament.api.gameView.watcher(pov.game)
              data   <- env.api.roundApi.watcher(pov, users, tour, tv = onTv.some)
              cross  <- env.game.crosstableApi.withMatchup(game)
              champs <- env.tv.tv.getChampions
              page   <- renderPage(views.tv.index(channel, champs, pov, data, cross, history))
            yield Ok(page).noCache,
            api = _ => env.api.roundApi.watcher(pov, users, none, tv = onTv.some).dmap { Ok(_) }
          )

  def games = gamesChannel(Channel.Best.key)

  def gamesChannel(chanKey: String) = Open:
    Channel.byKey.get(chanKey).so { channel =>
      env.tv.tv.getChampions.zip(env.tv.tv.getGames(channel, 15)).flatMap { (champs, games) =>
        Ok.page(views.tv.games(channel, games.map(Pov.naturalOrientation), champs)).map(_.noCache)
      }
    }

  def gameChannelReplacement(chanKey: String, gameId: GameId, exclude: List[String]) = Open:
    val gameFu = Channel.byKey.get(chanKey).so { channel =>
      env.tv.tv.getReplacementGame(channel, gameId, exclude.map { GameId(_) })
    }
    Found(gameFu): game =>
      JsonOk:
        play.api.libs.json.Json.obj(
          "id"   -> game.id,
          "html" -> views.game.mini(Pov.naturalOrientation(game)).toString
        )

  def apiGamesChannel(chanKey: String) = Anon:
    Channel.byKey.get(chanKey).so { channel =>
      env.tv.tv.getGameIds(channel, getInt("nb").fold(10)(_.atMost(30).atLeast(1))).map { gameIds =>
        val config =
          lila.api.GameApiV2.ByIdsConfig(
            ids = gameIds,
            format = lila.api.GameApiV2.Format.byRequest(req),
            flags = gameC.requestPgnFlags(extended = false).copy(delayMoves = false),
            perSecond = MaxPerSecond(30)
          )
        noProxyBuffer(Ok.chunked(env.api.gameApiV2.exportByIds(config))).as(gameC.gameContentType(config))
      }
    }

  def feedDefault = Anon:
    serveFeedFromChannel(Channel.Best)

  def feed(chanKey: String) = Anon:
    Channel.byKey.get(chanKey).so(serveFeedFromChannel)

  private def serveFeedFromChannel(channel: Channel)(using Context): Fu[Result] =
    given timeout: akka.util.Timeout = akka.util.Timeout(1 second)
    import akka.pattern.ask
    import play.api.libs.EventSource
    import lila.tv.TvBroadcast
    val bc   = getBool("bc")
    val ctag = summon[scala.reflect.ClassTag[TvBroadcast.SourceType]]
    env.tv.channelBroadcasts
      .get(channel)
      .so: actor =>
        (actor ? TvBroadcast.Connect(bc)).mapTo(ctag).map { source =>
          if bc then
            Ok.chunked(source.via(EventSource.flow).log("Tv.feed"))
              .as(ContentTypes.EVENT_STREAM)
              .pipe(noProxyBuffer)
          else jsToNdJson(source)
        }

  def frameDefault = Anon:
    serveFrameFromChannel(Channel.Best)

  def frame(chanKey: String) = Anon:
    Channel.byKey.get(chanKey).so(serveFrameFromChannel)

  private def serveFrameFromChannel(channel: Channel)(using Context) =
    env.tv.tv
      .getGame(channel)
      .flatMap:
        _.fold(notFoundText()): g =>
          InEmbedContext:
            Ok(views.tv.embed(Pov.naturalOrientation(g), channel.key.some))
