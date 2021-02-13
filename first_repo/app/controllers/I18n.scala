package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json

import lila.app._
import lila.common.HTTPRequest

final class I18n(env: Env) extends LilaController(env) {

  private def toLang = lila.i18n.I18nLangPicker.byStr _

  private val form = Form(single("lang" -> text.verifying { code =>
    toLang(code).isDefined
  }))

  def select =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      form
        .bindFromRequest()
        .fold(
          _ => notFound,
          code => {
            val lang = toLang(code) err "Universe is collapsing"
            ctx.me.filterNot(_.lang contains lang.code).?? {
              env.user.repo.setLang(_, lang)
            } >> negotiate(
              html = {
                val redir = Redirect {
                  HTTPRequest.referer(ctx.req).fold(routes.Lobby.home.url) { str =>
                    try {
                      val pageUrl = new java.net.URL(str)
                      val path    = pageUrl.getPath
                      val query   = pageUrl.getQuery
                      if (query == null) path
                      else path + "?" + query
                    } catch {
                      case _: java.net.MalformedURLException => routes.Lobby.home.url
                    }
                  }
                }
                if (ctx.isAnon) redir.withCookies(env.lilaCookie.session("lang", lang.code))
                else redir
              }.fuccess,
              api = _ => Ok(Json.obj("lang" -> lang.code)).fuccess
            )
          }
        )
    }
}
