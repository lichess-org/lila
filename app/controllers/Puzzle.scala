package controllers

import play.api.libs.json._
import scala.util.chaining._
import views._

import lila.api.Context
import lila.app._
import lila.common.config.MaxPerSecond
import lila.puzzle.PuzzleTheme
import lila.puzzle.{ Result, PuzzleRound, Puzzle => Puz }

final class Puzzle(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  private def renderJson(puzzle: Puz, theme: PuzzleTheme, round: Option[PuzzleRound] = None)(implicit
      ctx: Context
  ): Fu[JsObject] =
    env.puzzle.jsonView(puzzle = puzzle, theme = theme.key, user = ctx.me, round = round)

  private def renderShow(puzzle: Puz, theme: PuzzleTheme)(implicit ctx: Context) =
    ctx.me.?? { env.puzzle.api.round.find(_, puzzle) } flatMap {
      renderShowWithRound(puzzle, theme, _)
    }

  private def renderShowWithRound(puzzle: Puz, theme: PuzzleTheme, round: Option[PuzzleRound])(implicit
      ctx: Context
  ) =
    ctx.me.?? { env.puzzle.api.round.find(_, puzzle) } flatMap { round =>
      renderJson(puzzle, theme, round) map { json =>
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
            html = renderShow(puzzle, PuzzleTheme.any),
            api = _ => renderJson(puzzle, PuzzleTheme.any) map { Ok(_) }
          ) map NoCache
        }
      }
    }

  def home =
    Open { implicit ctx =>
      NoBot {
        val theme = PuzzleTheme.any
        nextPuzzleForMe(theme.key) flatMap {
          renderShowWithRound(_, theme, none)
        }
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

  private def nextPuzzleForMe(theme: PuzzleTheme.Key)(implicit ctx: Context): Fu[Puz] =
    ctx.me match {
      case Some(me) => env.puzzle.session.nextPuzzleFor(me, theme)
      case None     => env.puzzle.anon.getOneFor(theme) orFail "Couldn't find a puzzle for anon!"
    }

  def complete(themeStr: String, id: String) =
    OpenBody { implicit ctx =>
      NoBot {
        implicit val req = ctx.body
        OptionFuResult(env.puzzle.api.puzzle find Puz.Id(id)) { puzzle =>
          val theme = PuzzleTheme findOrAny themeStr
          lila.mon.puzzle.round.attempt(ctx.isAuth, theme.key.value).increment()
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
                        theme = theme.key,
                        user = me,
                        result = Result(resultInt == 1),
                        isStudent = isStudent
                      )
                      _ = env.puzzle.session.onComplete(round, theme.key)
                      next     <- nextPuzzleForMe(theme.key)
                      nextJson <- renderJson(next, theme)
                    } yield Ok(
                      Json.obj(
                        "round" -> Json
                          .obj(
                            "win"        -> round.win,
                            "ratingDiff" -> (perf.intRating - me.perfs.puzzle.intRating)
                          )
                          .add("vote" -> round.vote),
                        "next" -> nextJson
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

  def themes = Open { implicit ctx =>
    env.puzzle.api.theme.categorizedWithCount map { themes =>
      Ok(views.html.puzzle.theme.list(themes))
    }
  }

  def show(themeOrId: String) = Open { implicit ctx =>
    NoBot {
      PuzzleTheme.find(themeOrId) match {
        case None if themeOrId.size == 5 =>
          OptionFuResult(env.puzzle.api.puzzle find Puz.Id(themeOrId)) { renderShow(_, PuzzleTheme.any) }
        case None => Redirect(routes.Puzzle.home()).fuccess
        case Some(theme) =>
          nextPuzzleForMe(theme.key) flatMap {
            renderShowWithRound(_, theme, none)
          }
      }
    }
  }

  def showWithTheme(themeKey: String, id: String) = Open { implicit ctx =>
    NoBot {
      val theme = PuzzleTheme.findOrAny(themeKey)
      OptionFuResult(env.puzzle.api.puzzle find Puz.Id(id)) { renderShow(_, theme) }
    }
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
