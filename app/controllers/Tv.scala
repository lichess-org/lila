package controllers

import play.api.http.ContentTypes
import scala.util.chaining.*
import play.api.libs.json.*
import views.*

import lila.app.{ given, * }
import lila.game.Pov
import lila.tv.Tv.Channel
import lila.common.Json.given

final class Tv(env: Env, apiC: => Api, gameC: => Game) extends LilaController(env):

  def index     = Open(serveIndex)
  def indexLang = LangPage(routes.Tv.index)(serveIndex)

  private def serveIndex(using Context) = serveChannel(Channel.Best.key)

  def onChannel(chanKey: String) = Open(serveChannel(chanKey))

  private def serveChannel(chanKey: String)(using Context) =
    Channel.byKey.get(chanKey) so lichessTv

  def sides(gameId: GameId, color: String) = Open:
    Found(chess.Color.fromName(color) so { env.round.proxyRepo.pov(gameId, _) }): pov =>
      env.game.crosstableApi.withMatchup(pov.game) flatMap { ct =>
        Ok.page(html.tv.side.sides(pov, ct))
      }

  private given Writes[lila.tv.Tv.Champion] = Json.writes

  def channels = apiC.ApiRequest:
    env.tv.tv.getChampions map {
      _.channels map { (chan, champ) => chan.key -> champ }
    } map { Json.toJson(_) } dmap Api.ApiResult.Data.apply

  private def lichessTv(channel: Channel)(using Context) =
    Found(env.tv.tv getGameAndHistory channel): (game, history) =>
      val flip    = getBool("flip")
      val natural = Pov naturalOrientation game
      val pov     = if flip then !natural else natural
      val onTv    = lila.round.OnTv.Lichess(channel.key, flip)
      env.user.api
        .gamePlayers(game.userIdPair, game.perfType)
        .flatMap: users =>
          gameC.preloadUsers(users)
          negotiateApi(
            html = for
              tour   <- env.tournament.api.gameView.watcher(pov.game)
              data   <- env.api.roundApi.watcher(pov, users, tour, tv = onTv.some)
              cross  <- env.game.crosstableApi.withMatchup(game)
              champs <- env.tv.tv.getChampions
              page   <- renderPage(html.tv.index(channel, champs, pov, data, cross, history))
            yield Ok(page).noCache,
            api = _ => env.api.roundApi.watcher(pov, users, none, tv = onTv.some) dmap { Ok(_) }
          )

  def games = gamesChannel(Channel.Best.key)

  def gamesChannel(chanKey: String) = Open:
    Channel.byKey.get(chanKey).so { channel =>
      env.tv.tv.getChampions zip env.tv.tv.getGames(channel, 15) flatMap { (champs, games) =>
        Ok.page(html.tv.games(channel, games map Pov.naturalOrientation, champs)).map(_.noCache)
      }
    }

  def gameChannelReplacement(chanKey: String, gameId: GameId, exclude: List[String]) = Open:
    val gameFu = Channel.byKey.get(chanKey) so { channel =>
      env.tv.tv.getReplacementGame(channel, gameId, exclude map { GameId(_) })
    }
    Found(gameFu): game =>
      JsonOk:
        play.api.libs.json.Json.obj(
          "id"   -> game.id,
          "html" -> views.html.game.mini(Pov naturalOrientation game).toString
        )

  def apiGamesChannel(chanKey: String) = Anon:
    Channel.byKey.get(chanKey) so { channel =>
      env.tv.tv.getGameIds(channel, getInt("nb").fold(10)(_ atMost 30 atLeast 1)) map { gameIds =>
        val config =
          lila.api.GameApiV2.ByIdsConfig(
            ids = gameIds,
            format = lila.api.GameApiV2.Format byRequest req,
            flags = gameC.requestPgnFlags(extended = false).copy(delayMoves = false),
            perSecond = lila.common.config.MaxPerSecond(30)
          )
        noProxyBuffer(Ok.chunked(env.api.gameApiV2.exportByIds(config))).as(gameC.gameContentType(config))
      }
    }

  def feed = Anon:
    import makeTimeout.short
    import akka.pattern.ask
    import lila.round.TvBroadcast
    import play.api.libs.EventSource
    val bc   = getBool("bc")
    val ctag = summon[scala.reflect.ClassTag[TvBroadcast.SourceType]]
    env.round.tvBroadcast ? TvBroadcast.Connect(bc) mapTo ctag map { source =>
      if bc then
        Ok.chunked(source via EventSource.flow log "Tv.feed")
          .as(ContentTypes.EVENT_STREAM) pipe noProxyBuffer
      else apiC.sourceToNdJson(source)
    }

  def frame = Anon:
    env.tv.tv.getBestGame.flatMap:
      _.fold(notFoundText()): game =>
        InEmbedContext:
          Ok(views.html.tv.embed(Pov naturalOrientation game))
