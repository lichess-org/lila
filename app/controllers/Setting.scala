package controllers

import lila._
import views._
import http.{ Context, BodyContext, Setting => HttpSetting }

import play.api.data.Form
import play.api.mvc.Cookie
import scalaz.effects._

object Setting extends LilaController {

  def userRepo = env.user.userRepo
  def forms = user.DataForm

  def set(name: String) = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    val setter = name match {
      case "theme" ⇒ setTheme.some
      case "sound" ⇒ setSound.some
      case "chat" ⇒ setChat.some
      case "bg" ⇒ setBg.some
      case _       ⇒ none
    }
    setter.fold({
      case (form, process) ⇒
        FormResult(form) { value ⇒
          Ok("ok") withCookies {
            process(HttpSetting(ctx), value).unsafePerformIO
          }
        }
    }, NotFound)
  }

  type Setter = (Form[String], (HttpSetting, String) ⇒ IO[Cookie])

  val setTheme: Setter = forms.theme -> {
    (setting, v) ⇒ setting.theme(v)(userRepo)
  }

  val setSound: Setter = forms.sound -> {
    (setting, v) ⇒ setting.sound(v)(userRepo)
  }

  val setChat: Setter = forms.chat -> {
    (setting, v) ⇒ setting.chat(v)(userRepo)
  }

  val setBg: Setter = forms.bg -> {
    (setting, v) ⇒ setting.bg(v)(userRepo)
  }
}
