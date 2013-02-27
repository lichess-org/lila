package controllers

import lila._
import views._
import http.{ Context, BodyContext, Setting ⇒ HttpSetting }

import play.api.data.Form
import play.api.mvc.Cookie
import scalaz.effects._

object Setting extends LilaController {

  private def userRepo = env.user.userRepo
  private def forms = user.DataForm

  def set(name: String) = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    (setters get name).fold(NotFound) {
      case (form, process) ⇒
        FormResult(form) { value ⇒
          Ok() withCookies process(HttpSetting(ctx), value).unsafePerformIO
        }
    }
  }

  private type Setter = (Form[String], (HttpSetting, String) ⇒ IO[Cookie])

  private lazy val setters = Map(
    "theme" -> setTheme,
    "sound" -> setSound,
    "chat" -> setChat,
    "bg" -> setBg)

  private val setTheme: Setter = forms.theme -> {
    (setting, v) ⇒ setting.theme(v)(userRepo)
  }

  private val setSound: Setter = forms.sound -> {
    (setting, v) ⇒ setting.sound(v)(userRepo)
  }

  private val setChat: Setter = forms.chat -> {
    (setting, v) ⇒ setting.chat(v)(userRepo)
  }

  private val setBg: Setter = forms.bg -> {
    (setting, v) ⇒ setting.bg(v)(userRepo)
  }
}
