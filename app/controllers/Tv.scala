package controllers

import play.api.http.ContentTypes
import scala.util.chaining.*
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.game.Pov
import Api.ApiResult

final class Tv(
    env: Env,
    apiC: => Api,
    gameC: => Game
) extends LilaController(env):

  def index     = Open(serveIndex(_))
  def indexLang = LangPage(routes.Tv.index)(serveIndex(_))
  private def serveIndex(implicit ctx: Context) =
    serveChannel(lila.tv.Tv.Channel.Best.key)

  def onChannel(chanKey: String) = Open(serveChannel(chanKey)(_))

  private def serveChannel(chanKey: String)(implicit ctx: Context) =
    lila.tv.Tv.Channel.byKey.get(chanKey) ?? lichessTv

  def sides(gameId: GameId, color: String) =
    Open { implicit ctx =>
      OptionFuResult(chess.Color.fromName(color) ?? { env.round.proxyRepo.pov(gameId, _) }) { pov =>
        env.game.crosstableApi.withMatchup(pov.game) map { ct =>
          Ok(html.tv.side.sides(pov, ct))
        }
      }
    }

  import play.api.libs.json.*
  import lila.common.Json.given
  given Writes[lila.tv.Tv.Champion] = Json.writes

  def channels =
    apiC.ApiRequest { _ =>
      env.tv.tv.getChampions map {
        _.channels map { case (chan, champ) => chan.name -> champ }
      } map { Json.toJson(_) } dmap ApiResult.Data.apply
    }

  private def lichessTv(channel: lila.tv.Tv.Channel)(implicit ctx: Context) =
    OptionFuResult(env.tv.tv getGameAndHistory channel) { case (game, history) =>
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
    }

  def games = gamesChannel(lila.tv.Tv.Channel.Best.key)

  def gamesChannel(chanKey: String) =
    Open { implicit ctx =>
      lila.tv.Tv.Channel.byKey.get(chanKey) ?? { channel =>
        env.tv.tv.getChampions zip env.tv.tv.getGames(channel, 15) map { case (champs, games) =>
          Ok(html.tv.games(channel, games map Pov.naturalOrientation, champs)).noCache
        }
      }
    }

  def gameChannelReplacement(chanKey: String, gameId: GameId, exclude: List[String]) =
    Open { implicit ctx =>
      val gameFu = lila.tv.Tv.Channel.byKey.get(chanKey) ?? { channel =>
        env.tv.tv.getReplacementGame(channel, gameId, exclude map { GameId(_) })
      }
      OptionResult(gameFu) { game =>
        JsonOk {
          play.api.libs.json.Json.obj(
            "id"   -> game.id,
            "html" -> views.html.game.mini(Pov naturalOrientation game).toString
          )
        }
      }
    }

  def apiGamesChannel(chanKey: String) =
    Action.async { req =>
      lila.tv.Tv.Channel.byKey.get(chanKey) ?? { channel =>
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
    }

  def feed =
    Action.async { req =>
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
    }

  def frame =
    Action.async { implicit req =>
      env.tv.tv.getBestGame map {
        case None       => NotFound
        case Some(game) => Ok(views.html.tv.embed(Pov naturalOrientation game))
      }
    }
