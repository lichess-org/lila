package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._

import lila.api.Context
import lila.app._
import views._

final class Appeal(env: Env) extends LilaController(env) {

  // def form =
  //   Auth { implicit ctx => _ =>
  //       env.appeal.forms.create map {
  //         case (form, captcha) => Ok(html.report.form(form, user, captcha))
  //       }
  //     }
  //   }
}
