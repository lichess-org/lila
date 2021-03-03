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
        Ok(html.racer.home).fuccess
      }
    }

  def create(sri: String) =
    Open { implicit ctx =>
      NoBot {
        env.racer.api.create(Socket.Sri(sri), ctx.me) map { race =>
          Redirect(routes.Racer.show(race.id.value))
        }
      }
    }

  def show(id: String) =
    Open { implicit ctx =>
      NoBot {
        env.racer.api.get(RacerRace.Id(id)) flatMap {
          _ ?? { race =>
            Ok(html.racer.show(race)).fuccess
          }
        }
      }
    }
}
