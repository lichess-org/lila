package controllers

import lila._
import views._
import http.BodyContext

object Setting extends LilaController {

  def userRepo = env.user.userRepo
  def forms = user.DataForm

  val color = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    FormResult[String](forms.color) { name ⇒
      Ok("ok") withCookies {
        http.Setting(ctx).color(name)(userRepo).unsafePerformIO
      }
    }
  }

  val sound = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    FormResult[String](forms.sound) { v ⇒
      Ok("ok") withCookies {
        http.Setting(ctx).sound(v)(userRepo).unsafePerformIO
      }
    }
  }
}
