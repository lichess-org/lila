package controllers

import scala.util.{ Try, Success, Failure }

import play.api.mvc._
import play.api.templates.Html

import lila.app._
import lila.puzzle.{ Generated, Puzzle ⇒ PuzzleModel }
import lila.user.{ User ⇒ UserModel, UserRepo }
import views._

object Puzzle extends LilaController {

  private def env = Env.puzzle

  def home = Open { implicit ctx =>
    env.api latest 50 map { puzzles =>
      Ok(views.html.puzzle.home(puzzles))
    }
  }

  def show(id: String) = Open { implicit ctx =>
    OptionOk(env.api find id) { puzzle =>
      views.html.puzzle.show(puzzle)
    }
  }

  def importBatch = Action.async(parse.json) { implicit req ⇒
    env.api.importBatch(req.body, ~get("token", req)) match {
      case Success(f) ⇒ f inject Ok("kthxbye")
      case Failure(e) ⇒ {
        play.api.Logger("Puzzle import").warn(e.getMessage)
        fuccess(BadRequest(e.getMessage))
      }
    }
  }
}
