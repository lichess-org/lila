package controllers

import play.api.data.Form
import play.api.libs.json.Json

import lila.app._
import lila.common.{ LilaCookie, HTTPRequest }

object I18n extends LilaController {

  def select = OpenBody { implicit ctx =>
    import play.api.data.Forms._
    import play.api.data._
    implicit val req = ctx.body
    Form(single("lang" -> text.verifying(Env.i18n.pool contains _))).bindFromRequest.fold(
      _ => notFound,
      lang => {
        ctx.me.filterNot(_.lang contains lang) ?? { me =>
          lila.user.UserRepo.setLang(me.id, lang)
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
          if (ctx.isAnon) redir.withCookies(LilaCookie.session("lang", lang))
          else redir
        }.fuccess,
        api = _ => Ok(Json.obj("lang" -> lang)).fuccess
      )
    )
  }
}
