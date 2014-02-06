package controllers

import scala.util.{ Try, Success, Failure }

import play.api.mvc._
import play.api.templates.Html

import lila.app._
import lila.puzzle.PuzzleId
import lila.puzzle.{ Generated, Puzzle ⇒ PuzzleModel }
import lila.user.{ User ⇒ UserModel, UserRepo }
import views._

object Puzzle extends LilaController {

  private def env = Env.puzzle

  def home = Open { implicit ctx ⇒
    env.api latest 50 map { puzzles ⇒
      Ok(views.html.puzzle.home(puzzles))
    }
  }

  def show(id: PuzzleId) = Open { implicit ctx ⇒
    OptionFuOk(env.api find id) { puzzle ⇒
      (ctx.userId ?? { env.api.attempt.find(puzzle.id, _) }) map { attempt ⇒
        views.html.puzzle.show(puzzle, attempt)
      }
    }
  }

  def attempt(id: PuzzleId) = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    OptionFuResult(env.api find id) { puzzle ⇒
      env.forms.attempt.bindFromRequest.fold(
        err ⇒ fuccess(BadRequest(err.toString)),
        data ⇒ ctx.me match {
          case Some(me) ⇒ env.finisher(puzzle, me, data) map { attempt ⇒
            Ok(views.html.puzzle.attempt(puzzle, attempt))
          }
          case None ⇒ env.finisher.anon(puzzle, data) inject Ok("ok")
        })
    }
  }

  def importBatch = Action.async(parse.json) { implicit req ⇒
    env.api.importBatch(req.body, ~get("token", req)) map { _ ⇒
      Ok("kthxbye")
    } recover {
      case e ⇒
        play.api.Logger("Puzzle import").warn(e.getMessage)
        BadRequest(e.getMessage)
    }
  }
}
