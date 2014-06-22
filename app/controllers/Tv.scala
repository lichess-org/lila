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

  private def lichessTv(implicit ctx: Context) = OptionFuResult(Env.tv.featured.one) { game =>
    Env.round.version(game.id) zip
      (GameRepo onTv 10) zip
      Env.game.crosstableApi(game) zip
      Env.tv.streamsOnAir zip
      (game.tournamentId ?? TournamentRepo.byId) map {
        case ((((v, games), cross), streams), tour) =>
          val flip = getBool("flip")
          Ok(html.tv.index(
            flip.fold(Pov second game, Pov first game),
            v, games, streams, tour, cross, flip))
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
      s"""document.write("<iframe src='http://$url&embed=" + document.domain + "' class='lichess-tv-iframe' allowtransparency='true' frameBorder='0' style='width: 226px; height: 266px;' title='Lichess free online chess'></iframe>");"""
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
