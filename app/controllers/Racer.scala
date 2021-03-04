package controllers

import play.api.mvc._
import views._

import lila.api.Context
import lila.app._
import lila.socket.Socket
import lila.racer.RacerRace

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
        HTTPRequest sid ctx.req match {
          case None => Redirect(routes.Racer.home).fuccess
          case Some(sid) =>
            env.racer.api.create(sid, ctx.me) map { race =>
              Redirect(routes.Racer.show(race.id.value))
            }
        }
      }
    }

  def show(id: String) =
    Open { implicit ctx =>
      NoBot {
        env.racer.api.withPuzzles(RacerRace.Id(id)) flatMap {
          _ ?? { case RacerRace.WithPuzzles(race, puzzles) =>
            Ok(
              ???
              // html.racer.show(
              //   race,
              //   env.racer.json(racer, puzzles),
              //   env.storm.json.pref(ctx.pref.copy(coords = lila.pref.Pref.Coords.NONE))
              // )
            ).fuccess
          }
        }
      }
    }
}
