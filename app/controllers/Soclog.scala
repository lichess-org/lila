package controllers

import lila.api.Context
import lila.app._
import views._

import play.api.mvc._

object Soclog extends LilaController {

  val api = Env.soclog.api

  import lila.soclog.AuthResult._

  def start(providerName: String) = Open { implicit ctx =>
    implicit val req = ctx.req
    WithProvider(providerName)(api.start)
  }

  def finish(providerName: String) = Open { implicit ctx =>
    implicit val req = ctx.req
    WithProvider(providerName) { provider =>
      api.finish(provider).fold(notFound) { profile =>
        println(profile)
        Ok(profile.toString).fuccess
      }
    }
  }

  private def WithProvider(name: String)(f: lila.soclog.Provider => Fu[Result])(implicit ctx: Context) =
    Env.soclog.providers(name).fold(notFound)(f)
}
