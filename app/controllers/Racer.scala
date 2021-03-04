package controllers

import play.api.mvc._
import views._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.racer.RacerPlayer
import lila.racer.RacerRace
import lila.socket.Socket

final class Racer(env: Env)(implicit mat: akka.stream.Materializer) extends LilaController(env) {

  def home =
    Open { implicit ctx =>
      NoBot {
        Ok(html.racer.home).fuccess map env.lilaCookie.ensure(ctx.req)
      }
    }

  def create =
    WithPlayerId { implicit ctx => playerId =>
      env.racer.api.create(playerId) map { race =>
        Redirect(routes.Racer.show(race.id.value))
      }
    }

  def show(id: String) =
    WithPlayerId { implicit ctx => playerId =>
      env.racer.api.get(RacerRace.Id(id)) match {
        case None => Redirect(routes.Racer.home).fuccess
        case Some(race) =>
          Ok(
            html.racer.show(
              race,
              env.racer.json.raceJson(race, playerId),
              env.storm.json.pref(ctx.pref.copy(coords = lila.pref.Pref.Coords.NONE))
            )
          ).fuccess
      }
    }

  def join(id: String) =
    WithPlayerId { implicit ctx => playerId =>
      Redirect {
        env.racer.api.join(RacerRace.Id(id), playerId) match {
          case None       => routes.Racer.home
          case Some(race) => routes.Racer.show(race.id.value)
        }
      }.fuccess
    }

  private def WithPlayerId(f: Context => RacerPlayer.Id => Fu[Result]): Action[Unit] =
    Open { implicit ctx =>
      NoBot {
        HTTPRequest sid ctx.req map { env.racer.api.playerId(_, ctx.me) } match {
          case None     => Redirect(routes.Racer.home).fuccess
          case Some(id) => f(ctx)(id)
        }
      }
    }
}
