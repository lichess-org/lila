package controllers

import play.api.libs.json._
import scala.util.chaining._
import views._

import lila.api.BodyContext
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
            html = renderShow(puzzle, PuzzleTheme.mix),
            api = v => renderJson(puzzle, PuzzleTheme.mix, apiVersion = v.some) dmap { Ok(_) }
          ) map NoCache
        }
      }
    }

  def home =
    Open { implicit ctx =>
      NoBot {
        val theme = PuzzleTheme.mix
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
        onComplete(Puz.Id(id), PuzzleTheme findOrAny themeStr, mobileBc = false)
      }
    }

  def mobileBcRound(nid: Long) =
    OpenBody { implicit ctx =>
      onComplete(Puz.numericalId(nid), PuzzleTheme.mix, mobileBc = true)
    }

  private def onComplete[A](id: Puz.Id, theme: PuzzleTheme, mobileBc: Boolean)(implicit
      ctx: BodyContext[A]
  ) =
    OptionFuResult(env.puzzle.api.puzzle find id) { puzzle =>
      implicit val req = ctx.body
      lila.mon.puzzle.round.attempt(ctx.isAuth, theme.key.value).increment()
      env.puzzle.forms.round
        .bindFromRequest()
        .fold(
          jsonFormError,
          resultInt =>
            {
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
                    json <-
                      if (mobileBc) fuccess {
                        Json
                          .obj(
                            "user" -> Json
                              .obj(
                                "rating" -> perf.intRating,
                                "recent" -> Json.arr(
                                  Json.arr(
                                    Puz.numericalId(puzzle.id),
                                    perf.intRating - me.perfs.puzzle.intRating,
                                    puzzle.glicko.intRating
                                  )
                                )
                              ),
                            "round" -> Json.obj(
                              "ratingDiff" -> 0,
                              "win"        -> (resultInt == 1)
                            ),
                            "voted" -> round.vote
                          )
                      }
                      else
                        for {
                          next     <- nextPuzzleForMe(theme.key)
                          nextJson <- renderJson(next, theme, newUser.some)
                        } yield Json.obj(
                          "round" -> env.puzzle.jsonView.roundJson(me, round, perf),
                          "next"  -> nextJson
                        )
                  } yield json
                case None =>
                  env.puzzle.finisher.incPuzzlePlays(puzzle)
                  if (mobileBc) fuccess(Json.obj("user" -> false))
                  else
                    nextPuzzleForMe(theme.key) flatMap {
                      renderJson(_, theme)
                    } map { json =>
                      Json.obj("next" -> json)
                    }
              }
            } dmap { json => Ok(json) as JSON }
        )
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

  def themes = Open { implicit ctx =>
    env.puzzle.api.theme.categorizedWithCount map { themes =>
      Ok(views.html.puzzle.theme.list(themes))
    }
  }

  def show(themeOrId: String) = Open { implicit ctx =>
    NoBot {
      PuzzleTheme.find(themeOrId) match {
        case None if themeOrId.size == 5 =>
          OptionFuResult(env.puzzle.api.puzzle find Puz.Id(themeOrId)) { renderShow(_, PuzzleTheme.mix) }
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

  def mobileBcLoad(nid: Long) =
    Open { implicit ctx =>
      negotiate(
        html = notFound,
        _ =>
          OptionFuOk(env.puzzle.api.puzzle find Puz.numericalId(nid)) { puz =>
            env.puzzle.jsonView.bc(puzzle = puz, theme = PuzzleTheme.mix, user = ctx.me)
          }.dmap(_ as JSON)
      )
    }

  // XHR load next play puzzle
  def mobileBcNew =
    Open { implicit ctx =>
      NoBot {
        negotiate(
          html = notFound,
          api = v => {
            val theme = PuzzleTheme.mix
            nextPuzzleForMe(theme.key) flatMap { puzzle =>
              renderJson(puzzle, theme, apiVersion = v.some)
            } dmap { Ok(_) }
          }
        )
      }
    }

  /* Mobile API: select a bunch of puzzles for offline use */
  def mobileBcBatchSelect =
    Auth { implicit ctx => me =>
      negotiate(
        html = notFound,
        api = v => {
          val nb = getInt("nb") getOrElse 15 atLeast 1 atMost 30
          env.puzzle.batch.nextFor(ctx.me, nb) flatMap { puzzles =>
            env.puzzle.jsonView.bc.batch(puzzles, ctx.me)
          } dmap { Ok(_) }
        }
      )
    }

  /* Mobile API: tell the server about puzzles solved while offline */
  def mobileBcBatchSolve =
    AuthBody(parse.json) { implicit ctx => me =>
      negotiate(
        html = notFound,
        api = v => {
          import lila.puzzle.PuzzleForm.bc._
          ctx.body.body
            .validate[SolveData]
            .fold(
              err => BadRequest(err.toString).fuccess,
              data =>
                data.solutions.lastOption
                  .?? { solution =>
                    env.puzzle.api.puzzle.find(Puz.numericalId(solution.id)).map2(_ -> Result(solution.win))
                  }
                  .flatMap {
                    case None =>
                      Ok(
                        Json.obj(
                          "user" -> Json.obj("rating" -> me.perfs.puzzle.intRating, "recent" -> Json.arr())
                        )
                      ).fuccess
                    case Some((puzzle, result)) =>
                      for {
                        isStudent <- env.clas.api.student.isStudent(me.id)
                        (round, perf) <- env.puzzle.finisher(
                          puzzle = puzzle,
                          theme = PuzzleTheme.mix.key,
                          user = me,
                          result = result,
                          isStudent = isStudent
                        )
                        _ = env.puzzle.session.onComplete(round, PuzzleTheme.mix.key)
                      } yield Ok(
                        Json.obj(
                          "user" -> Json.obj("rating" -> perf.intRating, "recent" -> Json.arr())
                        )
                      )
                  }
            )
        }
      )
    }

  def mobileBcVote(nid: Long) =
    AuthBody { implicit ctx => me =>
      negotiate(
        html = notFound,
        api = v => {
          implicit val req = ctx.body
          env.puzzle.forms.bc.vote
            .bindFromRequest()
            .fold(
              jsonFormError,
              intVote => {
                val vote = intVote == 1
                lila.mon.puzzle.vote(vote).increment()
                env.puzzle.api.vote.update(Puz.numericalId(nid), me, vote) inject jsonOkResult
              }
            )
        }
      )
    }
}
