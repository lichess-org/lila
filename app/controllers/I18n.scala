package controllers

import play.api.libs.json.Json

import lila.app.{ *, given }

final class I18n(env: Env) extends LilaController(env):

  def select = OpenBody:
    bindForm(lila.i18n.LangForm.select)(
      _ => notFound,
      code =>
        val lang = lila.i18n.LangPicker.byStr(code).err("Universe is collapsing")
        ctx.me.filterNot(_.lang contains lang.code).so {
          env.user.repo.setLang(_, lang)
        } >> negotiate(
          html =
            val redir = Redirect:
              lila.common.HTTPRequest
                .referer(ctx.req)
                .fold(routes.Lobby.home.url): str =>
                  try
                    val pageUrl = new java.net.URI(str).parseServerAuthority().toURL()
                    val path = pageUrl.getPath
                    val query = pageUrl.getQuery
                    if query == null then path
                    else path + "?" + query
                  catch case _: Exception => routes.Lobby.home.url
            if ctx.isAnon
            then redir.withCookies(env.security.lilaCookie.session("lang", lang.code))
            else redir
          ,
          Ok(Json.obj("lang" -> lang.code))
        )
    )
