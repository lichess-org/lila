package controllers

import play.api.libs.json._
import scala.util.chaining._
import views._

import lila.api.Context
import lila.app._
import lila.common.config.MaxPerSecond
import lila.puzzle.{ Result, Puzzle => Puz }

final class Puzzle(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  private def renderJson(
      puzzle: Puz,
      round: Option[lila.puzzle.Round] = None
  )(implicit ctx: Context): Fu[JsObject] =
    env.puzzle.jsonView(
      puzzle = puzzle,
      user = ctx.me,
      round = round,
      mobileApi = ctx.mobileApiVersion
    )

  private def renderShow(puzzle: Puz)(implicit ctx: Context) =
    ctx.me.?? { env.puzzle.api.round.find(_, puzzle) } flatMap { round =>
      renderJson(puzzle, round) map { json =>
        EnableSharedArrayBuffer(
          Ok(views.html.puzzle.show(puzzle, data = json, pref = env.puzzle.jsonView.pref(ctx.pref)))
        )
      }
    }

  def daily =
    Open { implicit ctx =>
      NoBot {
        OptionFuResult(env.puzzle.daily.get flatMap {
          _.map(_.id) ?? env.puzzle.api.puzzle.find
        }) { puzzle =>
          negotiate(
            html = renderShow(puzzle),
            api = _ => puzzleJson(puzzle) map { Ok(_) }
          ) map NoCache
        }
      }
    }

  def home =
    Open { implicit ctx =>
      NoBot {
        ???
        // env.puzzle.selector(ctx.me) flatMap { puzzle =>
        //   renderShow(puzzle, if (ctx.isAuth) "play" else "try")
        // }
      }
    }

  def show(id: Puz.Id) =
    Open { implicit ctx =>
      NoBot {
        OptionFuResult(env.puzzle.api.puzzle find id)(renderShow)
      }
    }

  def load(id: Puz.Id) =
    Open { implicit ctx =>
      NoBot {
        XhrOnly {
          OptionFuOk(env.puzzle.api.puzzle find id)(puzzleJson) map (_ as JSON)
        }
      }
    }

  private def puzzleJson(puzzle: Puz)(implicit ctx: Context) =
    renderJson(puzzle, round = none)

  // XHR load next play puzzle
  def newPuzzle =
    Open { implicit ctx =>
      NoBot {
        XhrOnly {
          ???
          // env.puzzle.selector(ctx.me) flatMap puzzleJson map { json =>
          //   Ok(json) as JSON
          // }
        }
      }
    }

  // new API
  def round2(id: Puz.Id) =
    OpenBody { implicit ctx =>
      NoBot {
        ???
        // implicit val req = ctx.body
        // OptionFuResult(env.puzzle.api.puzzle find id) { puzzle =>
        //   lila.mon.puzzle.round.attempt(puzzle.mate, ctx.isAuth, "new")
        //   env.puzzle.forms.round
        //     .bindFromRequest()
        //     .fold(
        //       jsonFormError,
        //       resultInt =>
        //         ctx.me match {
        //           case Some(me) =>
        //             for {
        //               isStudent <- env.clas.api.student.isStudent(me.id)
        //               (round, mode) <- env.puzzle.finisher(
        //                 puzzle = puzzle,
        //                 user = me,
        //                 result = Result(resultInt == 1),
        //                 mobile = lila.api.Mobile.Api.requested(ctx.req),
        //                 isStudent = isStudent
        //               )
        //               me2   <- if (mode.rated) env.user.repo byId me.id map (_ | me) else fuccess(me)
        //               infos <- env.puzzle userInfos me2
        //               voted <- ctx.me.?? { env.puzzle.api.vote.value(puzzle.id, _) }
        //             } yield Ok(
        //               Json.obj(
        //                 "user"  -> lila.puzzle.JsonView.infos(isOldMobile = false)(infos),
        //                 "round" -> lila.puzzle.JsonView.round(round),
        //                 "voted" -> voted
        //               )
        //             )
        //           case None =>
        //             env.puzzle.finisher.incPuzzleAttempts(puzzle)
        //             Ok(Json.obj("user" -> false)).fuccess
        //         }
        //     ) map (_ as JSON)
        // }
      }
    }

  def vote(id: Puz.Id) =
    AuthBody { implicit ctx => me =>
      NoBot {
        ???
        // implicit val req = ctx.body
        // env.puzzle.forms.vote
        //   .bindFromRequest()
        //   .fold(
        //     jsonFormError,
        //     vote =>
        //       env.puzzle.api.vote.find(id, me) flatMap { v =>
        //         env.puzzle.api.vote.update(id, me, v, vote == 1)
        //       } map { case (p, a) =>
        //         if (vote == 1) lila.mon.puzzle.vote.up.increment()
        //         else lila.mon.puzzle.vote.down.increment()
        //         Ok(Json.arr(a.value, p.vote.sum))
        //       }
        //   ) map (_ as JSON)
      }
    }

  /* Mobile API: select a bunch of puzzles for offline use */
  def batchSelect =
    Auth { implicit ctx => me =>
      negotiate(
        html = notFound,
        api = _ => ???
        // for {
        //   puzzles <- env.puzzle.batch.select(
        //     me,
        //     nb = getInt("nb") getOrElse 50 atLeast 1 atMost 100,
        //     after = getInt("after")
        //   )
        //   userInfo <- env.puzzle userInfos me
        //   json     <- env.puzzle.jsonView.batch(puzzles, userInfo)
        // } yield Ok(json) as JSON
      )
    }

  /* Mobile API: tell the server about puzzles solved while offline */
  def batchSolve =
    AuthBody(parse.json) { implicit ctx => me =>
      ???
    // import lila.puzzle.PuzzleBatch._
    // ctx.body.body
    //   .validate[SolveData]
    //   .fold(
    //     err => BadRequest(err.toString).fuccess,
    //     data =>
    //       negotiate(
    //         html = notFound,
    //         api = _ =>
    //           for {
    //             _     <- env.puzzle.batch.solve(me, data)
    //             me2   <- env.user.repo byId me.id map (_ | me)
    //             infos <- env.puzzle userInfos me2
    //           } yield Ok(
    //             Json.obj(
    //               "user" -> lila.puzzle.JsonView.infos(isOldMobile = false)(infos)
    //             )
    //           )
    //       )
    //   )
    }

  def frame =
    Action.async { implicit req =>
      env.puzzle.daily.get map {
        case None        => NotFound
        case Some(daily) => html.puzzle.embed(daily)
      }
    }

  def activity =
    Scoped(_.Puzzle.Read) { req => me =>
      val config = lila.puzzle.PuzzleActivity.Config(
        user = me,
        max = getInt("max", req) map (_ atLeast 1),
        perSecond = MaxPerSecond(20)
      )
      apiC
        .GlobalConcurrencyLimitPerIpAndUserOption(req, me.some)(env.puzzle.activity.stream(config)) {
          source =>
            Ok.chunked(source).as(ndJsonContentType) pipe noProxyBuffer
        }
        .fuccess
    }

}
