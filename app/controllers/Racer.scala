package controllers

import play.api.mvc.*
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.racer.RacerPlayer
import lila.racer.RacerRace
import play.api.libs.json.Json

final class Racer(env: Env) extends LilaController(env):

  def home     = Open(serveHome(_))
  def homeLang = LangPage(routes.Racer.home)(serveHome(_))
  private def serveHome(implicit ctx: Context) = NoBot {
    Ok(html.racer.home).toFuccess
  }

  def create =
    WithPlayerId { _ => playerId =>
      env.racer.api.createAndJoin(playerId) map { raceId =>
        Redirect(routes.Racer.show(raceId.value))
      }
    }

  def apiCreate = Scoped(_.Racer.Write) { _ => me =>
    me.noBot ?? {
      env.racer.api.createAndJoin(RacerPlayer.Id.User(me.id)) map { raceId =>
        JsonOk(
          Json.obj(
            "id"  -> raceId.value,
            "url" -> s"${env.net.baseUrl}${routes.Racer.show(raceId.value)}"
          )
        )
      }
    }
  }

  def show(id: String) =
    WithPlayerId { implicit ctx => playerId =>
      env.racer.api.get(RacerRace.Id(id)) match
        case None => Redirect(routes.Racer.home).toFuccess
        case Some(r) =>
          val race   = r.isLobby.??(env.racer.api.join(r.id, playerId)) | r
          val player = race.player(playerId) | env.racer.api.makePlayer(playerId)
          Ok(html.racer.show(env.racer.json.data(race, player, ctx.pref))).noCache.toFuccess
    }

  def rematch(id: String) =
    WithPlayerId { _ => playerId =>
      env.racer.api.get(RacerRace.Id(id)) match
        case None => Redirect(routes.Racer.home).toFuccess
        case Some(race) =>
          env.racer.api.rematch(race, playerId) map { rematchId =>
            Redirect(routes.Racer.show(rematchId.value))
          }
    }

  def lobby =
    WithPlayerId { _ => playerId =>
      env.racer.lobby.join(playerId) map { raceId =>
        Redirect(routes.Racer.show(raceId.value))
      }
    }

  private def WithPlayerId(f: Context => RacerPlayer.Id => Fu[Result]): Action[Unit] =
    Open { implicit ctx =>
      NoBot {
        ctx.req.sid map { env.racer.api.playerId(_, ctx.me) } match
          case Some(id) => f(ctx)(id)
          case None =>
            env.lilaCookie.ensureAndGet(ctx.req) { sid =>
              f(ctx)(env.racer.api.playerId(sid, none))
            }
      }
    }
