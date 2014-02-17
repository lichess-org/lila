package controllers

import scala.util.{ Try, Success, Failure }

import play.api.mvc._
import play.api.templates.Html

import lila.app._
import lila.puzzle.PuzzleId
import lila.puzzle.{ Generated, Puzzle => PuzzleModel }
import lila.user.{ User => UserModel, UserRepo }
import views._

object Puzzle extends LilaController {

  private def env = Env.puzzle

  def home = Open { implicit ctx =>
    env selector ctx.me zip env.userInfos(ctx.me) map {
      case (puzzle, infos) => Ok(views.html.puzzle.show(puzzle, infos, true))
    }
  }

  def show(id: PuzzleId) = Open { implicit ctx =>
    OptionFuOk(env.api.puzzle find id) { puzzle =>
      (env userInfos ctx.me) zip
        { ctx.me ?? (u => env.api.attempt.hasPlayed(u, puzzle) map (_.some)) } map {
          case (infos, played) => views.html.puzzle.show(puzzle, infos, played == Some(false))
        }
    }
  }

  // XHR load next play puzzle
  def newPuzzle = Open { implicit ctx =>
    env selector ctx.me zip (env userInfos ctx.me) map {
      case (puzzle, infos) => Ok(views.html.puzzle.playMode(puzzle, infos, true))
    }
  }

  def attempt(id: PuzzleId) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    OptionFuResult(env.api.puzzle find id) { puzzle =>
      env.forms.attempt.bindFromRequest.fold(
        err => fuccess(BadRequest(err.toString)),
        data => ctx.me match {
          case Some(me) => env.finisher(puzzle, me, data) flatMap {
            case (newAttempt, None) => UserRepo byId me.id map (_ | me) flatMap { me2 =>
              env.api.puzzle find id zip
                (env userInfos me2.some) zip
                (env.api.attempt hasVoted me2) map {
                  case ((p2, infos), voted) => Ok {
                    views.html.puzzle.viewMode(p2 | puzzle, newAttempt.some, infos, none, voted.some)
                  }
                }
            }
            case (oldAttempt, Some(win)) => env userInfos me.some map { infos =>
              Ok(views.html.puzzle.viewMode(puzzle, oldAttempt.some, infos, win.some))
            }
          }
          case None => fuccess {
            Ok(views.html.puzzle.viewMode(puzzle, none, none, data.isWin.some))
          }
        }
      )
    }
  }

  def vote(id: PuzzleId) = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      OptionFuResult(env.api.attempt.find(id, me.id)) { attempt =>
        env.forms.vote.bindFromRequest.fold(
          err => fuccess(BadRequest(err.toString)),
          vote => env.api.attempt.vote(attempt, vote == 1) map {
            case (p, a) => Ok(views.html.puzzle.vote(p, a.some))
          }
        )
      }
  }

  def importBatch = Action.async(parse.json) { implicit req =>
    env.api.puzzle.importBatch(req.body, ~get("token", req)) map { ids =>
      Ok("kthxbye " + ids.map(id => s"http://lichess.org/training/$id").mkString(" "))
    } recover {
      case e =>
        play.api.Logger("Puzzle import").warn(e.getMessage)
        BadRequest(e.getMessage)
    }
  }
}
