package lila.pref

import play.api.mvc.Cookie

import lila.common.LilaCookie
import lila.user.Context

final class SessionAware(api: PrefApi)(implicit ctx: Context) {

  def theme: Theme = Theme(get("theme").map(_ | Pref.default.theme).await)
  def theme(value: String) = set("theme", Theme(value).toString)

  def bg: String = ~get("dark").await.fold("light", "dark")
  def bg(value: String) = set("dark", value)

  private def get(name: String): Fu[Option[String]] =
    fuccess(ctx.req.session get name) orElse {
      ctx.me ?? { api.getPrefString(_, name) }
    } 

  private def set(name: String, value: String): Fu[Cookie] =
    ctx.me ?? { api.setPrefString(_, name, value) } inject
      LilaCookie.session(name, value.toString)(ctx.req)
}
