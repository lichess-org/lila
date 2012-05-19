package controllers

import lila._
import views._
import http.Context
import game.{ DbGame, Pov }
import round.Event
import socket.Util.connectionFail

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import scalaz.effects._

object Round extends LilaController {

  private val gameRepo = env.game.gameRepo
  private val socket = env.round.socket
  private val hand = env.round.hand
  private val rematcher = env.setup.rematcher

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

  def abort(fullId: String) = performAndRedirect(fullId, hand.abort)
  def resign(fullId: String) = performAndRedirect(fullId, hand.resign)
  def resignForce(fullId: String) = performAndRedirect(fullId, hand.resignForce)
  def drawClaim(fullId: String) = performAndRedirect(fullId, hand.drawClaim)
  def drawAccept(fullId: String) = performAndRedirect(fullId, hand.drawAccept)
  def drawOffer(fullId: String) = performAndRedirect(fullId, hand.drawOffer)
  def drawCancel(fullId: String) = performAndRedirect(fullId, hand.drawCancel)
  def drawDecline(fullId: String) = performAndRedirect(fullId, hand.drawDecline)
  def rematch(fullId: String) = Action {
    rematcher offerOrAccept fullId flatMap { validResult ⇒
      validResult.fold(
        err ⇒ putFailures(err) map { _ ⇒
          Redirect(routes.Round.player(fullId))
        }, {
          case (nextFullId, events) ⇒ performEvents(fullId)(events) map { _ ⇒
            Redirect(routes.Round.player(nextFullId))
          }
        }
      )
    } unsafePerformIO
  }
  def rematchCancel(fullId: String) = TODO
  def rematchDecline(fullId: String) = TODO
  def takebackAccept(fullId: String) = performAndRedirect(fullId, hand.takebackAccept)
  def takebackOffer(fullId: String) = performAndRedirect(fullId, hand.takebackOffer)
  def takebackCancel(fullId: String) = performAndRedirect(fullId, hand.takebackCancel)
  def takebackDecline(fullId: String) = performAndRedirect(fullId, hand.takebackDecline)

  def tableWatcher(gameId: String, color: String) = Open { implicit ctx ⇒
    IOption(gameRepo.pov(gameId, color)) { html.round.table.watch(_) }
  }

  def tablePlayer(fullId: String) = Open { implicit ctx ⇒
    IOption(gameRepo pov fullId) { pov ⇒
      pov.game.playable.fold(
        html.round.table.playing(pov),
        html.round.table.end(pov))
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

  type IOValidEvents = IO[Valid[List[Event]]]

  private def performAndRedirect(fullId: String, op: String ⇒ IOValidEvents) =
    Action {
      perform(fullId, op).unsafePerformIO
      Redirect(routes.Round.player(fullId))
    }

  private def perform(fullId: String, op: String ⇒ IOValidEvents): IO[Unit] =
    op(fullId) flatMap { validEvents ⇒
      validEvents.fold(putFailures, performEvents(fullId))
    }

  private def performEvents(fullId: String)(events: List[Event]): IO[Unit] =
    env.round.socket.send(DbGame takeGameId fullId, events)
}
