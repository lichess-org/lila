package controllers

import play.api.mvc._
import views._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
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
    Open { implicit ctx =>
      NoBot {
        WithSessionId { sid =>
          env.racer.api.create(sid, ctx.me) map { race =>
            Redirect(routes.Racer.show(race.id.value))
          }
        }
      }
    }

  def show(id: String) =
    Open { implicit ctx =>
      NoBot {
        WithSessionId { sid =>
          env.racer.api.get(RacerRace.Id(id)) match {
            case None => Redirect(routes.Racer.home).fuccess
            case Some(race) =>
              Ok(
                html.racer.show(
                  race,
                  env.racer.json.raceJson(race, ctx.me.toRight(sid)),
                  env.storm.json.pref(ctx.pref.copy(coords = lila.pref.Pref.Coords.NONE))
                )
              ).fuccess
          }
        }
      }
    }

  private def WithSessionId(f: String => Fu[Result])(implicit ctx: Context): Fu[Result] =
    HTTPRequest sid ctx.req match {
      case None      => Redirect(routes.Racer.home).fuccess
      case Some(sid) => f(sid)
    }
}
