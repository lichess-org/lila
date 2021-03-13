package controllers

import play.api.mvc._
import views._

import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.racer.RacerPlayer
import lila.racer.RacerRace
import lila.socket.Socket
import lila.common.LilaCookie

final class Racer(env: Env)(implicit mat: akka.stream.Materializer) extends LilaController(env) {

  def home =
    Open { implicit ctx =>
      NoBot {
        Ok(html.racer.home).fuccess
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
          val player = race.player(playerId) | RacerPlayer.make(playerId)
          Ok(
            html.racer.show(
              race,
              env.racer.json.data(race, player),
              env.storm.json.pref(ctx.pref.copy(coords = lila.pref.Pref.Coords.NONE))
            )
          ).fuccess
      }
    }

  def rematch(id: String) =
    WithPlayerId { implicit ctx => playerId =>
      env.racer.api.get(RacerRace.Id(id)) match {
        case None => Redirect(routes.Racer.home).fuccess
        case Some(race) =>
          env.racer.api.rematch(race, playerId) map { rematchId =>
            Redirect(routes.Racer.show(rematchId.value))
          }
      }
    }

  private def WithPlayerId(f: Context => RacerPlayer.Id => Fu[Result]): Action[Unit] =
    Open { implicit ctx =>
      NoBot {
        HTTPRequest sid ctx.req map { env.racer.api.playerId(_, ctx.me) } match {
          case Some(id) => f(ctx)(id)
          case None =>
            env.lilaCookie.ensureAndGet(ctx.req) { sid =>
              f(ctx)(env.racer.api.playerId(sid, none))
            }
        }
      }
    }
}
