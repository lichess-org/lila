package controllers

import lila._
import views._
import http.{ Context, BodyContext, Setting => HttpSetting }

import play.api.data.Form
import play.api.mvc.Cookie
import scalaz.effects._

object Setting extends LilaController {

  private def userRepo = env.user.userRepo
  private def forms = user.DataForm

  def set(name: String) = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    val setter = name match {
      case "theme" ⇒ setTheme.some
      case "sound" ⇒ setSound.some
      case "chat" ⇒ setChat.some
      case "bg" ⇒ setBg.some
      case _       ⇒ none
    }
    setter.fold(notFound) {
      case (form, process) ⇒
        FormResult(form) { value ⇒
          Ok("ok") withCookies {
            process(HttpSetting(ctx), value).unsafePerformIO
          }
        }
    }
  }

  private type Setter = (Form[String], (HttpSetting, String) ⇒ IO[Cookie])

  private lazy val setTheme: Setter = forms.theme -> {
    (setting, v) ⇒ setting.theme(v)(userRepo)
  }

  private lazy val setSound: Setter = forms.sound -> {
    (setting, v) ⇒ setting.sound(v)(userRepo)
  }

  private lazy val setChat: Setter = forms.chat -> {
    (setting, v) ⇒ setting.chat(v)(userRepo)
  }

  private lazy val setBg: Setter = forms.bg -> {
    (setting, v) ⇒ setting.bg(v)(userRepo)
  }
}
