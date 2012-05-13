package controllers

import lila._
import views._
import DataForm._
import http.BodyContext

object Setting extends LilaController {

  val color = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    FormResult[String](colorForm) { name ⇒
      Ok("ok") withSession {
        http.Setting(ctx).color(name)(env.userRepo).unsafePerformIO(req.session)
      }
    }
  }

  val sound = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    FormResult[String](soundForm) { v ⇒
      Ok("ok") withSession {
        http.Setting(ctx).sound(v)(env.userRepo).unsafePerformIO(req.session)
      }
    }
  }
}
