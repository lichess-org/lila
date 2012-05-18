package controllers

import lila._
import http.Context
import DataForm._
import chess.Color
import game.DbGame
import round.Event

import play.api._
import mvc._
import libs.concurrent.Akka
import libs.concurrent._
import Play.current
import libs.json._
import libs.iteratee._

import scalaz.effects._

object App extends LilaController {

  private val hand = env.round.hand

  def socket = WebSocket.async[JsValue] { implicit req ⇒
    implicit val ctx = Context(req, None)
    env.site.socket.join(
      uidOption = get("uid"),
      username = get("username"))
  }

  def abort(fullId: String) = performAndRedirect(fullId, hand.abort)

  def resign(fullId: String) = performAndRedirect(fullId, hand.resign)

  def resignForce(fullId: String) = performAndRedirect(fullId, hand.resignForce)

  def drawClaim(fullId: String) = performAndRedirect(fullId, hand.drawClaim)

  def drawAccept(fullId: String) = performAndRedirect(fullId, hand.drawAccept)

  def drawOffer(fullId: String) = performAndRedirect(fullId, hand.drawOffer)

  def drawCancel(fullId: String) = performAndRedirect(fullId, hand.drawCancel)

  def drawDecline(fullId: String) = performAndRedirect(fullId, hand.drawDecline)

  def takebackAccept(fullId: String) = performAndRedirect(fullId, hand.takebackAccept)

  def takebackOffer(fullId: String) = performAndRedirect(fullId, hand.takebackOffer)

  def takebackCancel(fullId: String) = performAndRedirect(fullId, hand.takebackCancel)

  def takebackDecline(fullId: String) = performAndRedirect(fullId, hand.takebackDecline)

  type IOValidEvents = IO[Valid[List[Event]]]

  private def performAndRedirect(fullId: String, op: String ⇒ IOValidEvents) =
    Action {
      perform(fullId, op).unsafePerformIO
      Redirect("/" + fullId)
    }

  private def perform(fullId: String, op: String ⇒ IOValidEvents): IO[Unit] =
    op(fullId) flatMap { validEvents ⇒
      validEvents.fold(putFailures, performEvents(fullId))
    }

  private def performEvents(fullId: String)(events: List[Event]): IO[Unit] =
    env.round.socket.send(DbGame takeGameId fullId, events)
}
