package controllers

import play.api.libs.json.Json
import play.api.mvc.*

import lila.app.{ *, given }
import lila.racer.{ RacerPlayer, RacerRace }

final class Racer(env: Env) extends LilaController(env):

  def home     = Open(serveHome)
  def homeLang = LangPage(routes.Racer.home)(serveHome)

  private def serveHome(using Context) = NoBot:
    Ok.page(views.racer.home)

  def create = WithPlayerId { _ ?=> playerId =>
    AuthOrTrustedIp:
      env.racer.api
        .createAndJoin(playerId)
        .map: raceId =>
          Redirect(routes.Racer.show(raceId.value))
  }

  def apiCreate = Scoped(_.Racer.Write) { _ ?=> me ?=>
    me.noBot.so:
      env.racer.api
        .createAndJoin(RacerPlayer.Id.User(me))
        .map: raceId =>
          JsonOk:
            Json.obj(
              "id"  -> raceId.value,
              "url" -> s"${env.net.baseUrl}${routes.Racer.show(raceId.value)}"
            )
  }

  def show(id: String) = WithPlayerId { ctx ?=> playerId =>
    env.racer.api.get(RacerRace.Id(id)) match
      case None    => Redirect(routes.Racer.home)
      case Some(r) =>
        val race   = r.isLobby.so(env.racer.api.join(r.id, playerId)) | r
        val player = race.player(playerId) | env.racer.api.makePlayer(playerId)
        Ok.page(views.racer.show(env.racer.json.data(race, player, ctx.pref))).map(_.noCache)
  }

  def rematch(id: String) = WithPlayerId { _ ?=> playerId =>
    AuthOrTrustedIp:
      env.racer.api.get(RacerRace.Id(id)) match
        case None       => Redirect(routes.Racer.home)
        case Some(race) =>
          env.racer.api
            .rematch(race, playerId)
            .map: rematchId =>
              Redirect(routes.Racer.show(rematchId.value))
  }

  def lobby = WithPlayerId { ctx ?=> playerId =>
    AuthOrTrustedIp:
      env.racer.lobby
        .join(playerId)
        .map: raceId =>
          Redirect(routes.Racer.show(raceId.value))
  }

  private def WithPlayerId(f: Context ?=> RacerPlayer.Id => Fu[Result]) = Open:
    NoBot:
      env.security.lilaCookie.ensureAndGet(ctx.req): sid =>
        f(env.racer.api.playerId(sid, ctx.me))
