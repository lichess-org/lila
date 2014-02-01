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

  def importBatch = Action.async(parse.json) { implicit req ⇒
    env.api.importBatch(req.body, ~get("token", req)) match {
      case Success(f) ⇒ f inject Ok("kthxbye")
      case Failure(e) ⇒ fuccess(BadRequest(e.getMessage))
    }
  }
}
