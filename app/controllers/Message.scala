package controllers

import lila._
import views._
import http.Context

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scalaz.effects._

object Message extends LilaController {

  def api = env.message.api
  def forms = env.message.forms

  def inbox(page: Int) = Auth { implicit ctx ⇒
    implicit me ⇒
      Ok(html.message.inbox(api.inbox(me, page)))
  }
  def thread(id: String) = TODO
  def form = TODO
  def create = TODO
  def delete(id: String) = TODO
}
