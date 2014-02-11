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
    env selector ctx.me zip env.userInfos(ctx.me) map {
      case (puzzle, infos) ⇒ Ok(views.html.puzzle.home(puzzle, infos))
    }
  }

  def show(id: PuzzleId) = Open { implicit ctx ⇒
    OptionFuOk(env.api.puzzle find id) { puzzle ⇒
      (env userInfos ctx.me) map { infos ⇒
        views.html.puzzle.show(puzzle, infos)
      }
    }
  }

  // XHR load next play puzzle
  def newPuzzle = Open { implicit ctx ⇒
    env selector ctx.me zip (env userInfos ctx.me) map {
      case (puzzle, infos) ⇒ Ok(views.html.puzzle.playMode(puzzle, infos, true))
    }
  }

  // XHR view
  def view(id: PuzzleId) = Open { implicit ctx ⇒
    val win = getInt("win", ctx.req) exists (1==)
    OptionFuOk(env.api.puzzle find id) { puzzle ⇒
      (ctx.userId ?? { env.api.attempt.find(puzzle.id, _) }) zip
        (env userInfos ctx.me) map {
          case (attempt, infos) ⇒
            views.html.puzzle.viewMode(puzzle, attempt, infos, win.some)
        }
    }
  }

  def attempt(id: PuzzleId) = OpenBody { implicit ctx ⇒
    implicit val req = ctx.body
    OptionFuResult(env.api.puzzle find id) { puzzle ⇒
      env.forms.attempt.bindFromRequest.fold(
        err ⇒ fuccess(BadRequest(err.toString)),
        data ⇒ ctx.me match {
          case Some(me) ⇒ env.finisher(puzzle, me, data) flatMap {
            case (newAttempt, None) ⇒ UserRepo byId me.id map (_ | me) flatMap { me2 ⇒
              env.api.puzzle find id zip (env userInfos me2.some) map {
                case (p2, infos) ⇒ Ok(views.html.puzzle.viewMode(p2 | puzzle, newAttempt.some, infos, none))
              }
            }
            case (oldAttempt, Some(win)) ⇒ env userInfos me.some map { infos ⇒
              Ok(views.html.puzzle.viewMode(puzzle, oldAttempt.some, infos, win.some))
            }
          }
          case None ⇒ fuccess {
            Ok(views.html.puzzle.viewMode(puzzle, none, none, data.isWin.some))
          }
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
