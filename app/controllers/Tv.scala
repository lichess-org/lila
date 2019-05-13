package controllers

import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.game.{ GameRepo, Pov }
import views._

object Tv extends LilaController {

  def index = onChannel(lila.tv.Tv.Channel.Best.key)

  def onChannel(chanKey: String) = Open { implicit ctx =>
    (lila.tv.Tv.Channel.byKey get chanKey).fold(notFound)(lichessTv)
  }

  def sides(gameId: String, color: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo.pov(gameId, color)) { pov =>
      Env.game.crosstableApi.withMatchup(pov.game) map { ct =>
        Ok(html.tv.side.sides(pov, ct))
      }
    }
  }

  def channels = Api.ApiRequest { implicit ctx =>
    import play.api.libs.json._
    implicit val championWrites = Json.writes[lila.tv.Tv.Champion]
    Env.tv.tv.getChampions map {
      _.channels map { case (chan, champ) => chan.name -> champ }
    } map { Json.toJson(_) } map Api.Data.apply
  }

  private def lichessTv(channel: lila.tv.Tv.Channel)(implicit ctx: Context) =
    OptionFuResult(Env.tv.tv getGameAndHistory channel) {
      case (game, history) =>
        val flip = getBool("flip")
        val pov = if (flip) Pov second game else Pov first game
        val onTv = lila.round.OnLichessTv(channel.key, flip)
        negotiate(
          html = {
            Env.api.roundApi.watcher(pov, lila.api.Mobile.Api.currentVersion, tv = onTv.some) zip
              Env.game.crosstableApi.withMatchup(game) zip
              Env.tv.tv.getChampions map {
                case data ~ cross ~ champions => NoCache {
                  Ok(html.tv.index(channel, champions, pov, data, cross, flip, history))
                }
              }
          },
          api = apiVersion => Env.api.roundApi.watcher(pov, apiVersion, tv = onTv.some) map { Ok(_) }
        )
    }

  def games = gamesChannel(lila.tv.Tv.Channel.Best.key)

  def gamesChannel(chanKey: String) = Open { implicit ctx =>
    (lila.tv.Tv.Channel.byKey get chanKey) ?? { channel =>
      Env.tv.tv.getChampions zip Env.tv.tv.getGames(channel, 12) map {
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
    Env.round.tvBroadcast ? TvBroadcast.GetEnumerator mapTo
      manifest[TvBroadcast.EnumeratorType] map { enum =>
        Ok.chunked(enum &> EventSource()).as("text/event-stream") |> noProxyBuffer
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
    Env.tv.tv.getBestGame map {
      case None => NotFound
      case Some(game) => Ok(views.html.tv.embed(Pov first game))
    }
  }
}
