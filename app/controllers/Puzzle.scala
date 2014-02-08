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
    env selector ctx.me map { puzzle ⇒
      Ok(views.html.puzzle.home(puzzle))
    }
  }

  def show(id: PuzzleId) = Open { implicit ctx ⇒
    OptionFuOk(env.api.puzzle find id) { puzzle ⇒
      (ctx.userId ?? { env.api.attempt.find(puzzle.id, _) }) map { attempt ⇒
        views.html.puzzle.show(puzzle, attempt)
      }
    }
  }

  // XHR load nex play puzzle
  def next = Open { implicit ctx ⇒
    env selector ctx.me map { puzzle ⇒
      Ok(views.html.puzzle.playMode(puzzle))
    }
  }

  // XHR view chunks
  def view(id: PuzzleId) = Open { implicit ctx ⇒
    OptionFuOk(env.api.puzzle find id) { puzzle ⇒
      (ctx.userId ?? { env.api.attempt.find(puzzle.id, _) }) map { attempt ⇒
        views.html.puzzle.view(puzzle, attempt)
      }
    }
  }

  def attempt(id: PuzzleId) = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    OptionFuResult(env.api.puzzle find id) { puzzle ⇒
      env.forms.attempt.bindFromRequest.fold(
        err ⇒ fuccess(BadRequest(err.toString)),
        data ⇒ (ctx.me match {
          case Some(me) ⇒ env.finisher(puzzle, me, data) map (_.some)
          case None     ⇒ env.finisher.anon(puzzle, data) inject none
        }) map { attempt ⇒
          Ok(views.html.puzzle.viewMode(puzzle, attempt))
        }
      )
    }
  }

  def vote(id: PuzzleId) = AuthBody { implicit ctx ⇒
    me ⇒
      implicit val req = ctx.body
      OptionFuResult(env.api.attempt.find(id, me.id)) { attempt ⇒
        env.forms.vote.bindFromRequest.fold(
          err ⇒ fuccess(BadRequest(err.toString)),
          vote ⇒ env.api.attempt.vote(attempt, vote == 1) map {
            case (p, a) ⇒ Ok(views.html.puzzle.vote(p, a.some))
          }
        )
      }
  }

  def importBatch = Action.async(parse.json) { implicit req ⇒
    env.api.puzzle.importBatch(req.body, ~get("token", req)) map { _ ⇒
      Ok("kthxbye")
    } recover {
      case e ⇒
        play.api.Logger("Puzzle import").warn(e.getMessage)
        BadRequest(e.getMessage)
    }
  }
}
