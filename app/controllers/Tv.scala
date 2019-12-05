package controllers

import play.api.http.ContentTypes
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.game.Pov
import views._

final class Tv(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  def index = onChannel(lila.tv.Tv.Channel.Best.key)

  def onChannel(chanKey: String) = Open { implicit ctx =>
    (lila.tv.Tv.Channel.byKey get chanKey).fold(notFound)(lichessTv)
  }

  def sides(gameId: String, color: String) = Open { implicit ctx =>
    OptionFuResult(chess.Color(color) ?? { env.round.proxyRepo.pov(gameId, _) }) { pov =>
      env.game.crosstableApi.withMatchup(pov.game) map { ct =>
        Ok(html.tv.side.sides(pov, ct))
      }
    }
  }

  def channels = apiC.ApiRequest { implicit ctx =>
    import play.api.libs.json._
    implicit val championWrites = Json.writes[lila.tv.Tv.Champion]
    env.tv.tv.getChampions map {
      _.channels map { case (chan, champ) => chan.name -> champ }
    } map { Json.toJson(_) } map Api.Data.apply
  }

  private def lichessTv(channel: lila.tv.Tv.Channel)(implicit ctx: Context) =
    OptionFuResult(env.tv.tv getGameAndHistory channel) {
      case (game, history) =>
        val flip = getBool("flip")
        val pov = if (flip) Pov second game else Pov first game
        val onTv = lila.round.OnLichessTv(channel.key, flip)
        negotiate(
          html = {
            env.api.roundApi.watcher(pov, lila.api.Mobile.Api.currentVersion, tv = onTv.some) zip
              env.game.crosstableApi.withMatchup(game) zip
              env.tv.tv.getChampions map {
                case data ~ cross ~ champions => NoCache {
                  Ok(html.tv.index(channel, champions, pov, data, cross, flip, history))
                }
              }
          },
          api = apiVersion => env.api.roundApi.watcher(pov, apiVersion, tv = onTv.some) map { Ok(_) }
        )
    }

  def games = gamesChannel(lila.tv.Tv.Channel.Best.key)

  def gamesChannel(chanKey: String) = Open { implicit ctx =>
    (lila.tv.Tv.Channel.byKey get chanKey) ?? { channel =>
      env.tv.tv.getChampions zip env.tv.tv.getGames(channel, 15) map {
        case (champs, games) => NoCache {
          Ok(html.tv.games(channel, games map lila.game.Pov.first, champs))
        }
      }
    }
  }

  def feed = Action.async { req =>
    import makeTimeout.short
    import akka.pattern.ask
    import lila.round.TvBroadcast
    import play.api.libs.EventSource
    env.round.tvBroadcast ? TvBroadcast.Connect mapTo
      manifest[TvBroadcast.SourceType] map { source =>
        Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM) |> noProxyBuffer
      }
  }

  /* for BC */
  def embed = Action { req =>
    Ok {
      val config = ui.EmbedConfig(req)
      val url = s"""${req.domain + routes.Tv.frame}?bg=${config.bg}&theme=${config.board}"""
      s"""document.write("<iframe src='https://$url&embed=" + document.domain + "' class='lichess-tv-iframe' allowtransparency='true' frameborder='0' style='width: 224px; height: 264px;' title='Lichess free online chess'></iframe>");"""
    } as JAVASCRIPT withHeaders (CACHE_CONTROL -> "max-age=86400")
  }

  def frame = Action.async { implicit req =>
    env.tv.tv.getBestGame map {
      case None => NotFound
      case Some(game) => Ok(views.html.tv.embed(Pov first game))
    }
  }
}
