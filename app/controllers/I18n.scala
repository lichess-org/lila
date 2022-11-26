package controllers

import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.Json

import lila.app.{ given, * }
import lila.common.HTTPRequest

final class I18n(env: Env) extends LilaController(env):

  private def toLang = lila.i18n.I18nLangPicker.byStr

  private val form = Form(single("lang" -> text.verifying { code =>
    toLang(code).isDefined
  }))

  def select =
    OpenBody { implicit ctx =>
      given play.api.mvc.Request[?] = ctx.body
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
              }.toFuccess,
              api = _ => Ok(Json.obj("lang" -> lang.code)).toFuccess
            )
          }
        )
    }
