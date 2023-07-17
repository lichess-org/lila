package controllers

import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.Json

import lila.app.{ given, * }

final class I18n(env: Env) extends LilaController(env):

  private def toLang = lila.i18n.I18nLangPicker.byStr

  private val form = Form(single("lang" -> text.verifying { code =>
    toLang(code).isDefined
  }))

  def select = OpenBody:
    form
      .bindFromRequest()
      .fold(
        _ => notFound,
        code =>
          val lang = toLang(code) err "Universe is collapsing"
          ctx.me.filterNot(_.lang contains lang.code).so {
            env.user.repo.setLang(_, lang)
          } >> negotiate(
            html =
              val redir = Redirect:
                ctx.req.referer.fold(routes.Lobby.home.url): str =>
                  try
                    val pageUrl = new java.net.URI(str).parseServerAuthority().toURL()
                    val path    = pageUrl.getPath
                    val query   = pageUrl.getQuery
                    if query == null then path
                    else path + "?" + query
                  catch case _: Exception => routes.Lobby.home.url
              if ctx.isAnon
              then redir.withCookies(env.lilaCookie.session("lang", lang.code))
              else redir
            ,
            Ok(Json.obj("lang" -> lang.code))
          )
      )
