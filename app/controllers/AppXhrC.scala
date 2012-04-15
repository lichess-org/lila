package lila
package controllers

import DataForm._
import chess.Color
import model.{ Event, DbGame }

import play.api._
import mvc._
import libs.concurrent.Akka
import Play.current
import libs.json._
import libs.iteratee._

import scalaz.effects._

object AppXhrC extends LilaController {

  private val hand = env.hand

  def socket(gameId: String, color: String) =
    WebSocket.async[JsValue] { implicit request ⇒
      env.gameSocket.join(
        gameId = gameId ~ { i ⇒ println("Attempt to connect to " + i) },
        colorName = color,
        uid = get("uid") err "Socket UID missing",
        version = getInt("version") err "Socket version missing",
        playerId = get("playerId"),
        username = get("username")).unsafePerformIO
    }

  def outoftime(fullId: String) = Action {
    IOk(perform(fullId, hand.outoftime))
  }

  def abort(fullId: String) = performAndRedirect(fullId, hand.abort)

  def resign(fullId: String) = performAndRedirect(fullId, hand.resign)

  def forceResign(fullId: String) = performAndRedirect(fullId, hand.forceResign)

  def drawClaim(fullId: String) = performAndRedirect(fullId, hand.drawClaim)

  def drawAccept(fullId: String) = performAndRedirect(fullId, hand.drawAccept)

  def drawOffer(fullId: String) = performAndRedirect(fullId, hand.drawOffer)

  def drawCancel(fullId: String) = performAndRedirect(fullId, hand.drawCancel)

  def drawDecline(fullId: String) = performAndRedirect(fullId, hand.drawDecline)

  def nbPlayers = Action { Ok(0) }

  def nbGames = Action { Ok(env.gameRepo.countPlaying.unsafePerformIO) }

  type IOValidEvents = IO[Valid[List[Event]]]

  private def perform(fullId: String, op: String ⇒ IOValidEvents): IO[Unit] =
    op(fullId) flatMap { res ⇒
      res.fold(
        failures ⇒ putStrLn(failures.list mkString "\n"),
        events ⇒ env.gameSocket.send(DbGame takeGameId fullId, events)
      )
    }

  private def performAndRedirect(fullId: String, op: String ⇒ IOValidEvents) =
    Action {
      perform(fullId, op).unsafePerformIO
      Redirect("/" + fullId)
    }
}
