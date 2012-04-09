package lila
package controllers

import DataForm._
import chess.Color

import play.api._
import mvc._
import libs.concurrent.Akka
import Play.current
import libs.json._
import libs.iteratee._

import scalaz.effects.IO

object AppXhrC extends LilaController {

  private val xhr = env.appXhr

  def socket(gameId: String, color: String) =
    WebSocket.async[JsValue] { implicit request ⇒
      env.gameSocket.join(
        gameId = gameId,
        colorName = color,
        uid = get("uid") err "Socket UID missing",
        version = getInt("version") err "Socket version missing",
        playerId = get("playerId"),
        username = get("username")).unsafePerformIO
    }

  def outoftime(fullId: String) = Action { ValidIOk(xhr outoftime fullId) }

  def abort(fullId: String) = validAndRedirect(fullId, xhr.abort)

  def resign(fullId: String) = validAndRedirect(fullId, xhr.resign)

  def forceResign(fullId: String) = validAndRedirect(fullId, xhr.forceResign)

  def drawClaim(fullId: String) = validAndRedirect(fullId, xhr.drawClaim)

  def drawAccept(fullId: String) = validAndRedirect(fullId, xhr.drawAccept)

  def drawOffer(fullId: String) = validAndRedirect(fullId, xhr.drawOffer)

  def drawCancel(fullId: String) = validAndRedirect(fullId, xhr.drawCancel)

  def drawDecline(fullId: String) = validAndRedirect(fullId, xhr.drawDecline)

  def moretime(fullId: String) = Action {
    (xhr moretime fullId).unsafePerformIO.fold(
      e ⇒ BadRequest(e.list mkString "\n"),
      time ⇒ Ok(time)
    )
  }

  def nbPlayers = Action { Ok(0) }

  def nbGames = Action { Ok(env.gameRepo.countPlaying.unsafePerformIO) }

  private def validAndRedirect(fullId: String, f: String ⇒ IO[Valid[Unit]]) =
    Action {
      ValidIORedir(f(fullId), fullId)
    }
}
