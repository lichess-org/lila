package controllers

import scala.util.{ Try, Success, Failure }

import play.api.mvc._
import play.api.templates.Html

import lila.app._
import lila.problem.{ Generated, Problem ⇒ ProblemModel }
import lila.user.{ User ⇒ UserModel, UserRepo }
import views._

object Problem extends LilaController {

  private def env = Env.problem

  def debug = Open { implicit ctx =>
    env.api latest 50 map { problems =>
      Ok(problems.toString)
    }
  }

  def home = Open { implicit ctx =>
    env.api latest 50 map { problems =>
      Ok(views.html.problem.home(problems))
    }
  }

  def show(id: String) = Open { implicit ctx =>
    OptionOk(env.api find id) { problem =>
      views.html.problem.show(problem)
    }
  }

  def importBatch = Action.async(parse.json) { implicit req ⇒
    env.api.importBatch(req.body, ~get("token", req)) match {
      case Success(f) ⇒ f inject Ok("kthxbye")
      case Failure(e) ⇒ {
        play.api.Logger("Problem import").warn(e.getMessage)
        fuccess(BadRequest(e.getMessage))
      }
    }
  }
}
