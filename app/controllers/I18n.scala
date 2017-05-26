package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.Lang
import play.api.libs.json.Json

import lila.app._
import lila.common.{ LilaCookie, HTTPRequest }

object I18n extends LilaController {

  private val form = Form(single("lang" -> text.verifying { code =>
    Lang.get(code) ?? lila.i18n.I18nDb.langs.contains
  }))

  def select = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    form.bindFromRequest.fold(
      _ => notFound,
      code => {
        val lang = Lang(code)
        ctx.me.filterNot(_.lang contains lang.code) ?? { me =>
          lila.user.UserRepo.setLang(me.id, lang.code)
        }
      } >> negotiate(
        html = {
          val redir = Redirect {
            s"${Env.api.Net.Protocol}${lang}.${Env.api.Net.Domain}" + {
              HTTPRequest.referer(ctx.req).fold(routes.Lobby.home.url) { str =>
                try {
                  val pageUrl = new java.net.URL(str);
                  val path = pageUrl.getPath
                  val query = pageUrl.getQuery
                  if (query == null) path
                  else path + "?" + query
                } catch {
                  case e: java.net.MalformedURLException => routes.Lobby.home.url
                }
              }
            }
          }
          if (ctx.isAnon) redir.withCookies(LilaCookie.session("lang", lang.code))
          else redir
        }.fuccess,
        api = _ => Ok(Json.obj("lang" -> lang.code)).fuccess
      )
    )
  }
}
