package controllers

import play.api.http.ContentTypes
import scala.util.chaining._
import views._

import lila.api.Context
import lila.app._
import lila.game.Pov

final class Tv(
    env: Env,
    apiC: => Api,
    gameC: => Game
) extends LilaController(env) {

  def index = onChannel(lila.tv.Tv.Channel.Best.key)

  def onChannel(chanKey: String) =
    Open { implicit ctx =>
      (lila.tv.Tv.Channel.byKey get chanKey).fold(notFound)(lichessTv)
    }

  def sides(gameId: String, color: String) =
    Open { implicit ctx =>
      OptionFuResult(chess.Color.fromName(color) ?? { env.round.proxyRepo.pov(gameId, _) }) { pov =>
        env.game.crosstableApi.withMatchup(pov.game) map { ct =>
          Ok(html.tv.side.sides(pov, ct))
        }
      }
    }

  def channels =
    apiC.ApiRequest { _ =>
      import play.api.libs.json._
      implicit val championWrites = Json.writes[lila.tv.Tv.Champion]
      env.tv.tv.getChampions map {
        _.channels map { case (chan, champ) => chan.name -> champ }
      } map { Json.toJson(_) } map Api.Data.apply
    }

  private def lichessTv(channel: lila.tv.Tv.Channel)(implicit ctx: Context) =
    OptionFuResult(env.tv.tv getGameAndHistory channel) { case (game, history) =>
      val flip    = getBool("flip")
      val natural = Pov naturalOrientation game
      val pov     = if (flip) !natural else natural
      val onTv    = lila.round.OnLichessTv(channel.key, flip)
      negotiate(
        html = env.tournament.api.gameView.watcher(pov.game) flatMap { tour =>
          env.api.roundApi.watcher(pov, tour, lila.api.Mobile.Api.currentVersion, tv = onTv.some) zip
            env.game.crosstableApi.withMatchup(game) zip
            env.tv.tv.getChampions map { case ((data, cross), champions) =>
              NoCache {
                Ok(html.tv.index(channel, champions, pov, data, cross, history))
              }
            }
        },
        api = apiVersion => env.api.roundApi.watcher(pov, none, apiVersion, tv = onTv.some) map { Ok(_) }
      )
    }

  def games = gamesChannel(lila.tv.Tv.Channel.Best.key)

  def gamesChannel(chanKey: String) =
    Open { implicit ctx =>
      lila.tv.Tv.Channel.byKey.get(chanKey) ?? { channel =>
        env.tv.tv.getChampions zip env.tv.tv.getGames(channel, 15) map { case (champs, games) =>
          NoCache {
            Ok(html.tv.games(channel, games map Pov.naturalOrientation, champs))
          }
        }
      }
    }

  def gameChannelReplacement(chanKey: String, gameId: String, exclude: List[String]) =
    Open { implicit ctx =>
      val gameFu = lila.tv.Tv.Channel.byKey.get(chanKey) ?? { channel =>
        env.tv.tv.getReplacementGame(channel, gameId, exclude)
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
      val bc = getBool("bc", req)
      env.round.tvBroadcast ? TvBroadcast.Connect(bc) mapTo
        manifest[TvBroadcast.SourceType] map { source =>
          if (bc) Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM) pipe noProxyBuffer
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
}
