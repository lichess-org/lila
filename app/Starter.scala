package lila.app

import akka.actor._
import scala.concurrent.Future
import play.api.Application
import play.api.libs.concurrent.Akka.system
import play.api.libs.concurrent.Execution.Implicits._

import lila.api.Env.{ current ⇒ apiEnv }

object Starter {

  def apply(implicit app: Application) {

    loginfo("[boot] starter")

    // let's eagerly instanciates most modules
    (Env.setup, Env.game, Env.gameSearch, Env.team, 
    Env.teamSearch, Env.forumSearch, Env.message) 

    system.actorOf(Props(new Renderer), name = apiEnv.RendererName)

    system.actorOf(Props(new Router(
      baseUrl = apiEnv.Net.BaseUrl,
      protocol = apiEnv.Net.Protocol,
      domain = apiEnv.Net.Domain
    )), name = apiEnv.RouterName)
  }
}
