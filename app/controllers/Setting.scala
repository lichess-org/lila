package controllers

import lila.app._
import views._

import play.api.data.Form
import play.api.mvc._, Results._

object Pref extends LilaController {

  def set(name: String) = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    (setters get name) ?? {
      case (form, fn) ⇒ FormResult(form) { v ⇒
        fn(UserSetting(ctx), v) map { Ok() withCookies _ }
      }
    }
  }

  private type Setter = (Form[String], (UserSetting, String) ⇒ Fu[Cookie])

  private def forms = Env.user.forms

  private lazy val setters = Map(
    "theme" -> setTheme,
    "bg" -> setBg)

  private lazy val setTheme: Setter = forms.theme -> {
    (setting, v) ⇒ setting.theme(v)
  }

  private lazy val setBg: Setter = forms.bg -> {
    (setting, v) ⇒ setting.bg(v)
  }
}
