package controllers

import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.game.{ GameRepo, Game => GameModel, Pov }
import lila.tournament.TournamentRepo
import views._

object Tv extends LilaController {

  private val defaultToStream = false

  def index = Open { implicit ctx =>
    if (defaultToStream) Env.tv.streamsOnAir flatMap {
      case Nil           => lichessTv
      case first :: rest => fuccess(Ok(html.tv.stream(first, rest)))
    }
    else lichessTv
  }

  def lichess = Open { implicit ctx =>
    lichessTv
  }

  def side(gameId: String, color: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo.pov(gameId, color)) { pov =>
      (GameRepo onTv 10) zip Env.tv.streamsOnAir map {
        case (games, streams) => Ok(html.tv.side(pov, games, streams))
      }
    }
  }

  private def lichessTv(implicit ctx: Context) = OptionFuResult(Env.tv.featured.one) { game =>
    val flip = getBool("flip")
    val pov = flip.fold(Pov second game, Pov first game)
    Env.api.roundApi.watcher(pov, lila.api.Mobile.Api.currentVersion, tv = flip.some) zip
      (GameRepo onTv 10) zip
      Env.game.crosstableApi(game) zip
      Env.tv.streamsOnAir zip
      (game.tournamentId ?? TournamentRepo.byId) map {
        case ((((data, games), cross), streams), tour) =>
          Ok(html.tv.index(pov, data, games, streams, tour, cross, flip))
      }
  }

  def streamIn(id: String) = Open { implicit ctx =>
    Env.tv.streamsOnAir flatMap { streams =>
      streams find (_.id == id) match {
        case None    => notFound
        case Some(s) => fuccess(Ok(html.tv.stream(s, streams filterNot (_.id == id))))
      }
    }
  }

  def streamOut = Action.async {
    import makeTimeout.short
    import akka.pattern.ask
    import lila.round.TvBroadcast
    import play.api.libs.EventSource
    implicit val encoder = play.api.libs.Comet.CometMessage.jsonMessages
    Env.round.tvBroadcast ? TvBroadcast.GetEnumerator mapTo
      manifest[TvBroadcast.EnumeratorType] map { enum =>
        Ok.chunked(enum &> EventSource()).as("text/event-stream")
      }
  }

  def embed = Action { req =>
    Ok {
      val bg = get("bg", req) | "light"
      val theme = get("theme", req) | "brown"
      val url = s"""${req.domain + routes.Tv.frame}?bg=$bg&theme=$theme"""
      s"""document.write("<iframe src='http://$url&embed=" + document.domain + "' class='lichess-tv-iframe' allowtransparency='true' frameBorder='0' style='width: 224px; height: 264px;' title='Lichess free online chess'></iframe>");"""
    } as JAVASCRIPT withHeaders (CACHE_CONTROL -> "max-age=86400")
  }

  def frame = Action.async { req =>
    Env.tv.featured.one map {
      case None => NotFound
      case Some(game) => Ok(views.html.tv.embed(
        game,
        get("bg", req) | "light",
        lila.pref.Theme(~get("theme", req)).cssClass
      ))
    }
  }
}
