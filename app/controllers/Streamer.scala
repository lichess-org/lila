package controllers

import play.api.mvc._

import lila.app._
import views._

object Streamer extends LilaController {

  private def api = Env.streamer.api

  def index(page: Int) = Open { implicit ctx =>
    Env.streamer.pager(page) map { pager =>
      ???
      // Ok(html.streamer.index(pager))
    }
  }
}
