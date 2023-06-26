package controllers

import play.api.http.ContentTypes
import scala.util.chaining._

import lila.api.Context
import lila.app._
import lila.game.Pov
import views._

final class Tv(
    env: Env,
    apiC: => Api,
    gameC: => Game
) extends LilaController(env) {

  def index = onChannel(lila.tv.Tv.Channel.Standard.key)

  def onChannel(chanKey: String) = {
    Open { implicit ctx =>
      (lila.tv.Tv.Channel.byKey get chanKey) ?? lishogiTv
    }
  }

  def sides(gameId: String, color: String) =
    Open { implicit ctx =>
      OptionFuResult(shogi.Color.fromName(color) ?? { env.round.proxyRepo.pov(gameId, _) }) { pov =>
        env.game.crosstableApi.withMatchup(pov.game) map { ct =>
          Ok(html.tv.side.sides(pov, ct))
        }
      }
    }

  def channels = {
    apiC.ApiRequest { _ =>
      import play.api.libs.json._
      implicit val championWrites = Json.writes[lila.tv.Tv.Champion]
      env.tv.tv.getChampions map {
        _.channels map { case (chan, champ) => chan.key -> champ }
      } map { Json.toJson(_) } dmap Api.Data.apply
    }
  }

  private def lishogiTv(channel: lila.tv.Tv.Channel)(implicit ctx: Context) = {
    OptionFuResult(env.tv.tv getGameAndHistory channel) { case (game, history) =>
      val flip = getBool("flip")
      val pov  = if (flip) Pov second game else Pov first game
      val onTv = lila.round.OnLishogiTv(channel.key, flip)
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
  }

  def games = gamesChannel(lila.tv.Tv.Channel.Standard.key)

  def gamesChannel(chanKey: String) =
    Open { implicit ctx =>
      lila.tv.Tv.Channel.byKey.get(chanKey) ?? { channel =>
        env.tv.tv.getChampions zip env.tv.tv.getGames(channel, 12) map { case (champs, games) =>
          Ok(html.tv.games(channel, games map Pov.first, champs)).noCache
        }
      }
    }

  def apiGamesChannel(chanKey: String) =
    Action.async { req =>
      lila.tv.Tv.Channel.byKey.get(chanKey) ?? { channel =>
        env.tv.tv.getGameIds(channel, getInt("nb", req).fold(10)(_ atMost 20)) map { gameIds =>
          val config =
            lila.api.GameApiV2.ByIdsConfig(
              ids = gameIds,
              format = lila.api.GameApiV2.Format byRequest req,
              flags = gameC.requestNotationFlags(req, extended = false).copy(delayMoves = false),
              perSecond = lila.common.config.MaxPerSecond(20)
            )
          noProxyBuffer(Ok.chunked(env.api.gameApiV2.exportByIds(config))).as(gameC.gameContentType(config))
        }
      }
    }

  def feed =
    Action.async {
      import makeTimeout.short
      import akka.pattern.ask
      import lila.round.TvBroadcast
      import play.api.libs.EventSource
      env.round.tvBroadcast ? TvBroadcast.Connect mapTo
        manifest[TvBroadcast.SourceType] map { source =>
          Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM) pipe noProxyBuffer
        }
    }

  def frame =
    Action.async { implicit req =>
      {
        env.tv.tv.getBestGame map {
          case None       => NotFound
          case Some(game) => Ok(views.html.tv.embed(Pov first game))
        }
      }
    }
}
