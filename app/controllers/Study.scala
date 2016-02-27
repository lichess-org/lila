package controllers

import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.duration._

import lila.app._
import views._

object Study extends LilaController {

  private def env = Env.study

  def show(id: String) = Open { implicit ctx =>
    ???
    // OptionFuResult(env.api byId id) { study =>
    //   Ok(html.study.show(study, data))
    // } map NoCache
  }

  def create = AuthBody { implicit ctx =>
    ???
  }
}
