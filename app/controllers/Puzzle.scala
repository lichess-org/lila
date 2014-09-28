package controllers

import scala.util.{ Try, Success, Failure }

import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.puzzle.PuzzleId
import lila.puzzle.{ Generated, Puzzle => PuzzleModel }
import lila.user.{ User => UserModel, UserRepo }
import views._
import views.html.puzzle.JsData

object Puzzle extends LilaController {

  private def env = Env.puzzle

  private def renderShow(puzzle: PuzzleModel, mode: String)(implicit ctx: Context) =
    env userInfos ctx.me map { infos =>
      views.html.puzzle.show(puzzle, infos, mode, animationDuration = env.AnimationDuration)
    }

  def home = Open { implicit ctx =>
    selectPuzzle(ctx.me) flatMap { puzzle =>
      renderShow(puzzle, "play") map { Ok(_) }
    }
  }

  def show(id: PuzzleId) = Open { implicit ctx =>
    OptionFuOk(env.api.puzzle find id) { puzzle =>
      (ctx.me ?? { env.api.attempt.hasPlayed(_, puzzle) }) flatMap { played =>
        renderShow(puzzle, played.fold("try", "play"))
      }
    }
  }

  def load(id: PuzzleId) = Open { implicit ctx =>
    OptionFuOk(env.api.puzzle find id) { puzzle =>
      (env userInfos ctx.me) zip
        (ctx.me ?? { env.api.attempt.hasPlayed(_, puzzle) }) map {
          case (infos, played) => JsData(puzzle, infos, played.fold("try", "play"), animationDuration = env.AnimationDuration)
        }
    } map (_ as JSON)
  }

  def history = Auth { implicit ctx =>
    me =>
      env userInfos me map { ui => Ok(views.html.puzzle.history(ui)) }
  }

  // XHR load next play puzzle
  def newPuzzle = Open { implicit ctx =>
    selectPuzzle(ctx.me) zip (env userInfos ctx.me) map {
      case (puzzle, infos) => Ok(JsData(puzzle, infos, "play", animationDuration = env.AnimationDuration)) as JSON
    }
  }

  def difficulty = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      env.forms.difficulty.bindFromRequest.fold(
        err => fuccess(BadRequest(err.errorsAsJson)),
        value => Env.pref.api.setPref(me, (p: lila.pref.Pref) => p.copy(puzzleDifficulty = value)) >> {
          reqToCtx(ctx.req) flatMap { newCtx =>
            selectPuzzle(newCtx.me) zip env.userInfos(newCtx.me) map {
              case (puzzle, infos) => Ok(JsData(puzzle, infos, "play", animationDuration = env.AnimationDuration)(newCtx))
            }
          }
        }
      ) map (_ as JSON)
  }

  private def selectPuzzle(user: Option[UserModel]) =
    Env.pref.api.getPref(user) flatMap { pref =>
      env.selector(user, pref.puzzleDifficulty)
    }

  def attempt(id: PuzzleId) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    OptionFuResult(env.api.puzzle find id) { puzzle =>
      env.forms.attempt.bindFromRequest.fold(
        err => fuccess(BadRequest(err.errorsAsJson)),
        data => ctx.me match {
          case Some(me) => env.finisher(puzzle, me, data) flatMap {
            case (newAttempt, None) => UserRepo byId me.id map (_ | me) flatMap { me2 =>
              env.api.puzzle find id zip
                (env userInfos me2.some) zip
                (env.api.attempt hasVoted me2) map {
                  case ((p2, infos), voted) => Ok {
                    JsData(p2 | puzzle, infos, "view",
                      attempt = newAttempt.some,
                      voted = voted.some,
                      animationDuration = env.AnimationDuration)
                  }
                }
            }
            case (oldAttempt, Some(win)) => env userInfos me.some map { infos =>
              Ok(JsData(puzzle, infos, "view",
                attempt = oldAttempt.some,
                win = win.some,
                animationDuration = env.AnimationDuration))
            }
          }
          case None => fuccess {
            Ok(JsData(puzzle, none, "view",
              win = data.isWin.some,
              animationDuration = env.AnimationDuration))
          }
        }
      ) map (_ as JSON)
    }
  }

  def vote(id: PuzzleId) = AuthBody { implicit ctx =>
    me =>
      implicit val req = ctx.body
      OptionFuResult(env.api.attempt.find(id, me.id)) { attempt =>
        env.forms.vote.bindFromRequest.fold(
          err => fuccess(BadRequest(err.errorsAsJson)),
          vote => env.api.attempt.vote(attempt, vote == 1) map {
            case (p, a) => Ok(play.api.libs.json.Json.arr(a.vote, p.vote.sum))
          }
        ) map (_ as JSON)
      }
  }

  // def importBatch = Action.async(parse.json) { implicit req =>
  //   env.api.puzzle.importBatch(req.body, ~get("token", req)) map { ids =>
  //     Ok("kthxbye " + ids.map {
  //       case Success(id) =>
  //         val url = s"http://lichess.org/training/$id"
  //         play.api.Logger("puzzle import").info(s"${req.remoteAddress} $url")
  //         url
  //       case Failure(err) =>
  //         play.api.Logger("puzzle import").info(s"${req.remoteAddress} ${err.getMessage}")
  //         err.getMessage
  //     }.mkString(" "))
  //   } recover {
  //     case e =>
  //       play.api.Logger("puzzle import").warn(e.getMessage)
  //       BadRequest(e.getMessage)
  //   }
  // }
}
