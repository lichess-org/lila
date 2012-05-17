package controllers

import lila._
import views._

import play.api.mvc._

object Round extends LilaController {

  val gameRepo = env.game.gameRepo
  val socket = env.round.socket

  def watcher(gameId: String, color: String) = TODO

  def player(fullId: String) = Open { implicit ctx ⇒
    IOption(gameRepo pov fullId) { pov ⇒
      html.round.player(pov, socket blockingVersion pov.gameId)
    }
  }

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

  def table(gameId: String, color: String, fullId: String) = TODO
  def players(gameId: String) = TODO
}
