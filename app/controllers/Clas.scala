package controllers

import com.github.ghik.silencer.silent
import play.api.mvc._
import play.api.data.Form
import play.api.libs.json._

import lila.api.Context
import lila.app._
import views._

final class Clas(
    env: Env
) extends LilaController(env) {

  def index = Secure(_.Teacher) { implicit ctx => me =>
    WithTeacher(me) { t =>
      Ok(views.html.clas.index(t)).fuccess
    }
  }

  private def WithTeacher(me: lila.user.User)(
      f: lila.clas.Teacher.WithUser => Fu[Result]
  ): Fu[Result] =
    env.clas.api withTeacherOrCreate me flatMap f
}
