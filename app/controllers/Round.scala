package controllers

import lila._
import views._
import http.Context
import game.Pov
import socket.Util.connectionFail

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._

object Round extends LilaController {

  val gameRepo = env.game.gameRepo
  val socket = env.round.socket

  def websocketWatcher(gameId: String, color: String) = WebSocket.async[JsValue] { req ⇒
    implicit val ctx = reqToCtx(req)
    socket.joinWatcher(
      gameId, color, getInt("version"), get("uid"), get("username")
    ).unsafePerformIO
  }

  def websocketPlayer(fullId: String) = WebSocket.async[JsValue] { req ⇒
    implicit val ctx = reqToCtx(req)
    socket.joinPlayer(
      fullId, getInt("version"), get("uid"), get("username")
    ).unsafePerformIO
  }

  def watcher(gameId: String, color: String) = Open { implicit ctx ⇒
    IOption(gameRepo.pov(gameId, color)) { pov ⇒
      html.round.watcher(pov, version(pov.gameId))
    }
  }

  def player(fullId: String) = Open { implicit ctx ⇒
    IOption(gameRepo pov fullId) { pov ⇒
      html.round.player(pov, version(pov.gameId))
    }
  }

  private def version(gameId: String): Int = socket blockingVersion gameId

  def abort(fullId: String) = TODO
  def resign(fullId: String) = TODO
  def resignForce(fullId: String) = TODO
  def drawClaim(fullId: String) = TODO
  def drawAccept(fullId: String) = TODO
  def drawOffer(fullId: String) = TODO
  def drawCancel(fullId: String) = TODO
  def drawDecline(fullId: String) = TODO
  def takebackAccept(fullId: String) = TODO
  def takebackOffer(fullId: String) = TODO
  def takebackCancel(fullId: String) = TODO
  def takebackDecline(fullId: String) = TODO

  def tableWatcher(gameId: String, color: String) = Open { implicit ctx ⇒
    IOption(gameRepo.pov(gameId, color)) { html.round.table.watch(_) }
  }

  def tablePlayer(fullId: String) = Open { implicit ctx ⇒
    IOption(gameRepo pov fullId) { pov ⇒
      pov.game.playable.fold(html.round.table.playing(pov), html.round.table.end(pov))
    }
  }

  def players(gameId: String) = Open { implicit ctx ⇒
    import templating.Environment.playerLink
    JsonIOk(gameRepo game gameId map { gameOption ⇒
      gameOption.fold(
        game ⇒ (game.players collect {
          case player if player.isHuman ⇒ player.color.name -> playerLink(player).text
        } toMap) ++ ctx.me.fold(me ⇒ Map("me" -> me.usernameWithElo), Map()),
        Map()
      )
    })
  }
}
