package controllers

import scala.util.{ Try, Success, Failure }

import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.Play.current
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.puzzle.PuzzleId
import lila.puzzle.{ Generated, Puzzle => PuzzleModel, UserInfos }
import lila.user.{ User => UserModel, UserRepo }
import views._

object Puzzle extends LilaController {

  private def env = Env.puzzle

  private def renderJson(
    puzzle: PuzzleModel,
    userInfos: Option[UserInfos],
    mode: String,
    voted: Option[Boolean],
    round: Option[lila.puzzle.Round] = None,
    win: Option[Boolean] = None)(implicit ctx: Context) = lila.puzzle.JsonView(
    puzzle = puzzle,
    userInfos = userInfos,
    mode = mode,
    animationDuration = env.AnimationDuration,
    pref = ctx.pref,
    isMobileApi = ctx.isMobileApi,
    win = win,
    voted = voted)

  private def renderShow(puzzle: PuzzleModel, mode: String)(implicit ctx: Context) =
    env userInfos ctx.me map { infos =>
      views.html.puzzle.show(puzzle, renderJson(
        puzzle = puzzle,
        userInfos = infos,
        mode = mode,
        voted = none))
    }

  def daily = Open { implicit ctx =>
    OptionFuResult(env.daily() flatMap {
      _.map(_.id) ?? env.api.puzzle.find
    }) { puzzle =>
      negotiate(
        html = renderShow(puzzle, "play") map { Ok(_) },
        api = _ => puzzleJson(puzzle) map { Ok(_) }
      ) map { NoCache(_) }
    }
  }

  def home = Open { implicit ctx =>
    env.selector(ctx.me) flatMap { puzzle =>
      renderShow(puzzle, ctx.isAuth.fold("play", "try")) map { Ok(_) }
    }
  }

  def show(id: PuzzleId) = Open { implicit ctx =>
    OptionFuOk(env.api.puzzle find id) { puzzle =>
      renderShow(puzzle, "play")
    }
  }

  def load(id: PuzzleId) = Open { implicit ctx =>
    XhrOnly {
      OptionFuOk(env.api.puzzle find id)(puzzleJson) map (_ as JSON)
    }
  }

  private def puzzleJson(puzzle: PuzzleModel)(implicit ctx: Context) =
    (env userInfos ctx.me) map { infos =>
      renderJson(puzzle, infos, "play", voted = none)
    }

  // XHR load next play puzzle
  def newPuzzle = Open { implicit ctx =>
    XhrOnly {
      env.selector(ctx.me) zip (env userInfos ctx.me) map {
        case (puzzle, infos) =>
          Ok(renderJson(puzzle, infos, ctx.isAuth.fold("play", "try"), voted = none)) as JSON
      } map (_ as JSON)
    }
  }

  def round(id: PuzzleId) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    OptionFuResult(env.api.puzzle find id) { puzzle =>
      if (puzzle.mate) lila.mon.puzzle.round.mate()
      else lila.mon.puzzle.round.material()
      env.forms.round.bindFromRequest.fold(
        err => fuccess(BadRequest(errorsAsJson(err))),
        data => ctx.me match {
          case Some(me) =>
            lila.mon.puzzle.round.user()
            env.finisher(puzzle, me, data) flatMap {
              case (newAttempt, None) => UserRepo byId me.id map (_ | me) flatMap { me2 =>
                env.api.puzzle find id zip
                  (env userInfos me2.some) zip
                  env.api.vote.value(id, me2) map {
                    case ((p2, infos), voted) => Ok {
                      renderJson(p2 | puzzle, infos, "view", voted = voted, round = newAttempt.some)
                    }
                  }
              }
              case (oldAttempt, Some(win)) => env.userInfos(me.some) zip
                ctx.me.?? { env.api.vote.value(puzzle.id, _) } map {
                  case (infos, voted) => Ok(renderJson(puzzle, infos, "view",
                    round = oldAttempt.some,
                    win = win.some,
                    voted = voted))
                }
            }
          case None => fuccess {
            lila.mon.puzzle.round.anon()
            Ok(renderJson(puzzle, none, "view", win = data.isWin.some, voted = none))
          }
        }
      ) map (_ as JSON)
    }
  }

  def vote(id: PuzzleId) = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    env.forms.vote.bindFromRequest.fold(
      err => fuccess(BadRequest(errorsAsJson(err))),
      vote => env.api.vote.find(id, me) flatMap {
        v => env.api.vote.update(id, me, v, vote == 1)
      } map {
        case (p, a) =>
          if (vote == 1) lila.mon.puzzle.vote.up()
          else lila.mon.puzzle.vote.down()
          Ok(Json.arr(a.value, p.vote.sum))
      }
    ) map (_ as JSON)
  }

  def recentGame = Action.async { req =>
    if (!get("token", req).contains(Env.api.apiToken)) BadRequest.fuccess
    else {
      import akka.pattern.ask
      import makeTimeout.short
      Env.game.recentGoodGameActor ? true mapTo manifest[Option[String]] flatMap {
        _ ?? lila.game.GameRepo.gameWithInitialFen flatMap {
          case Some((game, initialFen)) =>
            Ok(Env.api.pgnDump(game, initialFen.map(_.value)).toString).fuccess
          case _ =>
            lila.log("puzzle import").info("No recent good game, serving a random one :-/")
            lila.game.GameRepo.findRandomFinished(1000) flatMap {
              _ ?? { game =>
                lila.game.GameRepo.initialFen(game) map { fen =>
                  Ok(Env.api.pgnDump(game, fen).toString)
                }
              }
            }
        }
      }
    }
  }

  def importOne = Action.async(parse.json) { implicit req =>
    env.api.puzzle.importOne(req.body, ~get("token", req)) map { id =>
      val url = s"https://lichess.org/training/$id"
      lila.log("puzzle import").info(s"${req.remoteAddress} $url")
      Ok(s"kthxbye $url")
    } recover {
      case e =>
        lila.log("puzzle import").warn(s"${req.remoteAddress} ${e.getMessage}", e)
        BadRequest(e.getMessage)
    }
  }

  def embed = Action { req =>
    Ok {
      val bg = get("bg", req) | "light"
      val theme = get("theme", req) | "brown"
      val url = s"""${req.domain + routes.Puzzle.frame}?bg=$bg&theme=$theme"""
      s"""document.write("<iframe src='//$url&embed=" + document.domain + "' class='lichess-training-iframe' allowtransparency='true' frameBorder='0' style='width: 224px; height: 264px;' title='Lichess free online chess'></iframe>");"""
    } as JAVASCRIPT withHeaders (CACHE_CONTROL -> "max-age=86400")
  }

  def frame = Open { implicit ctx =>
    OptionOk(env.daily()) { daily =>
      html.puzzle.embed(daily)
    }
  }
}
