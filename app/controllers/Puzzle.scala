package controllers

import play.api.libs.json._
import scala.util.chaining._
import views._

import lila.api.Context
import lila.app._
import lila.common.config.MaxPerSecond
import lila.puzzle.{ Result, PuzzleRound, Puzzle => Puz }

final class Puzzle(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  private def renderJson(puzzle: Puz, round: Option[PuzzleRound] = None)(implicit
      ctx: Context
  ): Fu[JsObject] =
    env.puzzle.jsonView(
      puzzle = puzzle,
      user = ctx.me,
      round = round
    )

  private def renderShow(puzzle: Puz)(implicit ctx: Context) =
    ctx.me.?? { env.puzzle.api.round.find(_, puzzle) } flatMap {
      renderShowWithRound(puzzle, _)
    }

  private def renderShowWithRound(puzzle: Puz, round: Option[PuzzleRound])(implicit ctx: Context) =
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
            api = _ => renderJson(puzzle, none) map { Ok(_) }
          ) map NoCache
        }
      }
    }

  def home =
    Open { implicit ctx =>
      NoBot {
        nextPuzzleForMe flatMap {
          renderShowWithRound(_, none)
        }
      }
    }

  def show(id: String) =
    Open { implicit ctx =>
      NoBot {
        OptionFuResult(env.puzzle.api.puzzle find Puz.Id(id))(renderShow)
      }
    }

  def load(id: String) =
    Open { implicit ctx =>
      NoBot {
        XhrOnly {
          ???
          // OptionFuOk(env.puzzle.api.puzzle find Puz.Id(id))(puzzleJson _).dmap(_ as JSON)
        }
      }
    }

  // XHR load next play puzzle
  def newPuzzle =
    Open { implicit ctx =>
      NoBot {
        XhrOnly {
          nextPuzzleForMe flatMap { renderJson(_, none) } map { json =>
            Ok(json) as JSON
          }
        }
      }
    }

  private def nextPuzzleForMe(implicit ctx: Context): Fu[Puz] = ctx.me match {
    case None     => env.puzzle.anon.getOne orFail "Couldn't find a puzzle for anon!"
    case Some(me) => env.puzzle.cursor.nextPuzzleFor(me)
  }

  def round3(id: String) =
    OpenBody { implicit ctx =>
      NoBot {
        implicit val req = ctx.body
        OptionFuResult(env.puzzle.api.puzzle find Puz.Id(id)) { puzzle =>
          lila.mon.puzzle.round.attempt(ctx.isAuth).increment()
          env.puzzle.forms.round
            .bindFromRequest()
            .fold(
              jsonFormError,
              resultInt =>
                ctx.me match {
                  case Some(me) =>
                    for {
                      isStudent <- env.clas.api.student.isStudent(me.id)
                      (round, perf) <- env.puzzle.finisher(
                        puzzle = puzzle,
                        user = me,
                        result = Result(resultInt == 1),
                        isStudent = isStudent
                      )
                    } yield Ok(
                      Json.obj(
                        "perf" -> Json
                          .obj("rating" -> perf.intRating)
                          .add("provisional" -> perf.provisional),
                        "round" -> Json
                          .obj(
                            "win"        -> round.win,
                            "ratingDiff" -> (me.perfs.puzzle.intRating - perf.intRating)
                          )
                          .add("vote" -> round.vote)
                      )
                    )
                  case None =>
                    env.puzzle.finisher.incPuzzlePlays(puzzle)
                    fuccess(NoContent)
                }
            )
            .dmap(_ as JSON)
        }
      }
    }

  def vote(id: String) =
    AuthBody { implicit ctx => me =>
      NoBot {
        implicit val req = ctx.body
        env.puzzle.forms.vote
          .bindFromRequest()
          .fold(
            jsonFormError,
            vote =>
              env.puzzle.api.vote.update(Puz.Id(id), me, vote) map { newVote =>
                if (vote has true) lila.mon.puzzle.vote.up.increment()
                else if (vote has false) lila.mon.puzzle.vote.down.increment()
                jsonOkResult
              }
          )
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
