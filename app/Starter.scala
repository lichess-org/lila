package lila.app

import play.api.Application
import play.api.libs.concurrent.Akka.system
import akka.actor._

import lila.api.Env.{ current ⇒ apiEnv }

object Starter {

  def apply(implicit app: Application) {

    system.actorOf(Props(new Renderer), name = apiEnv.RendererName)
  }
}
