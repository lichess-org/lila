package controllers

import play.api.mvc._, Results._

import lila.app._
import lila.coach.{ Coach => CoachModel }
import lila.user.{ User => UserModel, UserRepo }
import views._

object Coach extends LilaController {

  private val api = Env.coach.api

  def index = Open { implicit ctx =>
    api.enabledWithUserList map { coaches =>
      Ok(html.coach.index(coaches))
    }
  }

  def show(username: String) = Open { implicit ctx =>
    OptionOk(api find username) { coach =>
      html.coach.show(coach)
    }
  }
}
