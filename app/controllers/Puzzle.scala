package controllers

import play.api.libs.json._
import scala.util.chaining._
import views._

import lila.api.Context
import lila.app._
import lila.common.ApiVersion
import lila.common.config.MaxPerSecond
import lila.puzzle.PuzzleTheme
import lila.puzzle.{ Result, PuzzleRound, PuzzleDifficulty, Puzzle => Puz }

final class Puzzle(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  private def renderJson(
      puzzle: Puz,
      theme: PuzzleTheme,
      newUser: Option[lila.user.User] = None,
      apiVersion: Option[ApiVersion] = None
  )(implicit
      ctx: Context
  ): Fu[JsObject] =
    if (apiVersion.exists(!_.puzzleV2))
      env.puzzle.jsonView.bc(puzzle = puzzle, theme = theme, user = newUser orElse ctx.me)
    else
      env.puzzle.jsonView(puzzle = puzzle, theme = theme, user = newUser orElse ctx.me)

  private def renderShow(puzzle: Puz, theme: PuzzleTheme)(implicit ctx: Context) =
    renderJson(puzzle, theme) zip
      ctx.me.??(u => env.puzzle.session.getDifficulty(u) dmap some) map { case (json, difficulty) =>
        EnableSharedArrayBuffer(
          Ok(views.html.puzzle.show(puzzle, json, env.puzzle.jsonView.pref(ctx.pref), difficulty))
        )
      }

  def daily =
    Open { implicit ctx =>
      NoBot {
        OptionFuResult(env.puzzle.daily.get flatMap {
          _.map(_.id) ?? env.puzzle.api.puzzle.find
        }) { puzzle =>
          negotiate(
            html = renderShow(puzzle, PuzzleTheme.any),
            api = v => renderJson(puzzle, PuzzleTheme.any, apiVersion = v.some) dmap { Ok(_) }
          ) map NoCache
        }
      }
    }

  def home =
    Open { implicit ctx =>
      NoBot {
        val theme = PuzzleTheme.any
        nextPuzzleForMe(theme.key) flatMap {
          renderShow(_, theme)
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
                      newUser = me.copy(perfs = me.perfs.copy(puzzle = perf))
                      _       = env.puzzle.session.onComplete(round, theme.key)
                      next     <- nextPuzzleForMe(theme.key)
                      nextJson <- renderJson(next, theme, newUser.some)
                    } yield Ok {
                      Json.obj(
                        "round" -> env.puzzle.jsonView.roundJson(me, round, perf),
                        "next"  -> nextJson
                      )
                    }
                  case None =>
                    env.puzzle.finisher.incPuzzlePlays(puzzle)
                    nextPuzzleForMe(theme.key) flatMap {
                      renderJson(_, theme)
                    } map { json =>
                      Ok(Json.obj("next" -> json))
                    }
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
            vote => {
              lila.mon.puzzle.vote(vote).increment()
              env.puzzle.api.vote.update(Puz.Id(id), me, vote) inject jsonOkResult
            }
          )
      }
    }

  def voteTheme(id: String, themeStr: String) =
    AuthBody { implicit ctx => me =>
      NoBot {
        PuzzleTheme.findDynamic(themeStr) ?? { theme =>
          implicit val req = ctx.body
          env.puzzle.forms.themeVote
            .bindFromRequest()
            .fold(
              jsonFormError,
              vote => {
                vote foreach { v => lila.mon.puzzle.voteTheme(theme.key.value, v).increment() }
                env.puzzle.api.theme.vote(me, Puz.Id(id), theme.key, vote) inject jsonOkResult
              }
            )
        }
      }
    }

  def setDifficulty(theme: String) =
    AuthBody { implicit ctx => me =>
      NoBot {
        implicit val req = ctx.body
        env.puzzle.forms.difficulty
          .bindFromRequest()
          .fold(
            jsonFormError,
            diff =>
              PuzzleDifficulty.find(diff) ?? { env.puzzle.session.setDifficulty(me, _) } inject
                Redirect(routes.Puzzle.show(theme))
          )
      }
    }

  def mobileBcLoad(nid: Long) =
    Open { implicit ctx =>
      negotiate(
        html = notFound,
        _ =>
          OptionFuOk(env.puzzle.api.puzzle find Puz.numericalId(nid)) { puz =>
            env.puzzle.jsonView.bc(puzzle = puz, theme = PuzzleTheme.any, user = ctx.me)
          }.dmap(_ as JSON)
      )
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
            renderShow(_, theme)
          }
      }
    }
  }

  def showWithTheme(themeKey: String, id: String) = Open { implicit ctx =>
    NoBot {
      val theme = PuzzleTheme.findOrAny(themeKey)
      OptionFuResult(env.puzzle.api.puzzle find Puz.Id(id)) { puzzle =>
        if (puzzle.themes contains theme.key) renderShow(puzzle, theme)
        else Redirect(routes.Puzzle.show(puzzle.id.value)).fuccess
      }
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
