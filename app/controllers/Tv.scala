package controllers

import play.api.http.ContentTypes
import scala.util.chaining.*
import play.api.libs.json.*
import views.*

import lila.api.Context
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
    Channel.byKey.get(chanKey) ?? lichessTv

  def sides(gameId: GameId, color: String) = Open:
    OptionFuResult(chess.Color.fromName(color) ?? { env.round.proxyRepo.pov(gameId, _) }) { pov =>
      env.game.crosstableApi.withMatchup(pov.game) map { ct =>
        Ok(html.tv.side.sides(pov, ct))
      }
    }

  private given Writes[lila.tv.Tv.Champion] = Json.writes

  def channels = apiC.ApiRequest:
    env.tv.tv.getChampions map {
      _.channels map { (chan, champ) => chan.name -> champ }
    } map { Json.toJson(_) } dmap Api.ApiResult.Data.apply

  private def lichessTv(channel: Channel)(using Context) =
    OptionFuResult(env.tv.tv getGameAndHistory channel): (game, history) =>
      val flip    = getBool("flip")
      val natural = Pov naturalOrientation game
      val pov     = if (flip) !natural else natural
      val onTv    = lila.round.OnTv.Lichess(channel.key, flip)
      negotiate(
        html = env.tournament.api.gameView.watcher(pov.game) flatMap { tour =>
          env.api.roundApi.watcher(pov, tour, lila.api.Mobile.Api.currentVersion, tv = onTv.some) zip
            env.game.crosstableApi.withMatchup(game) zip
            env.tv.tv.getChampions map { case ((data, cross), champions) =>
              Ok(html.tv.index(channel, champions, pov, data, cross, history)).noCache
            }
        },
        api = apiVersion => env.api.roundApi.watcher(pov, none, apiVersion, tv = onTv.some) dmap { Ok(_) }
      )

  def games = gamesChannel(Channel.Best.key)

  def gamesChannel(chanKey: String) = Open:
    Channel.byKey.get(chanKey) ?? { channel =>
      env.tv.tv.getChampions zip env.tv.tv.getGames(channel, 15) map { (champs, games) =>
        Ok(html.tv.games(channel, games map Pov.naturalOrientation, champs)).noCache
      }
    }

  def gameChannelReplacement(chanKey: String, gameId: GameId, exclude: List[String]) = Open:
    val gameFu = Channel.byKey.get(chanKey) ?? { channel =>
      env.tv.tv.getReplacementGame(channel, gameId, exclude map { GameId(_) })
    }
    OptionResult(gameFu): game =>
      JsonOk:
        play.api.libs.json.Json.obj(
          "id"   -> game.id,
          "html" -> views.html.game.mini(Pov naturalOrientation game).toString
        )

  def apiGamesChannel(chanKey: String) = Anon:
    Channel.byKey.get(chanKey) ?? { channel =>
      env.tv.tv.getGameIds(channel, getInt("nb", req).fold(10)(_ atMost 30 atLeast 1)) map { gameIds =>
        val config =
          lila.api.GameApiV2.ByIdsConfig(
            ids = gameIds,
            format = lila.api.GameApiV2.Format byRequest req,
            flags = gameC.requestPgnFlags(req, extended = false).copy(delayMoves = false),
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
    val bc   = getBool("bc", req)
    val ctag = summon[scala.reflect.ClassTag[TvBroadcast.SourceType]]
    env.round.tvBroadcast ? TvBroadcast.Connect(bc) mapTo ctag map { source =>
      if bc then
        Ok.chunked(source via EventSource.flow log "Tv.feed")
          .as(ContentTypes.EVENT_STREAM) pipe noProxyBuffer
      else apiC.sourceToNdJson(source)
    }

  def frame = Anon:
    env.tv.tv.getBestGame.mapz: game =>
      Ok(views.html.tv.embed(Pov naturalOrientation game))
