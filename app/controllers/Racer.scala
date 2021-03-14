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
      env.racer.api.createAndJoin(playerId) map { raceId =>
        Redirect(routes.Racer.show(raceId.value))
      }
    }

  def show(id: String) =
    WithPlayerId { implicit ctx => playerId =>
      env.racer.api.get(RacerRace.Id(id)) match {
        case None => Redirect(routes.Racer.home).fuccess
        case Some(r) =>
          val race   = r.isLobby.??(env.racer.api.join(r.id, playerId)) | r
          val player = race.player(playerId) | RacerPlayer.make(playerId)
          Ok(
            html.racer.show(
              race,
              env.racer.json.data(race, player),
              env.storm.json.pref(ctx.pref.copy(coords = lila.pref.Pref.Coords.NONE))
            )
          ).fuccess dmap NoCache
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

  def lobby =
    WithPlayerId { implicit ctx => playerId =>
      env.racer.lobby.join(playerId) map { raceId =>
        Redirect(routes.Racer.show(raceId.value))
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
