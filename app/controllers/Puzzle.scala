package controllers

import play.api.data.Form
import play.api.libs.json._
import scala.util.chaining._
import views._

import lila.api.BodyContext
import lila.api.Context
import lila.app._
import lila.common.ApiVersion
import lila.common.config.MaxPerSecond
import lila.puzzle.PuzzleForm.RoundData
import lila.puzzle.PuzzleTheme
import lila.puzzle.{ Result, PuzzleDifficulty, PuzzleReplay, PuzzleStreak, Puzzle => Puz }

final class Puzzle(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  private def renderJson(
      puzzle: Puz,
      theme: PuzzleTheme,
      replay: Option[PuzzleReplay] = None,
      newUser: Option[lila.user.User] = None,
      apiVersion: Option[ApiVersion] = None
  )(implicit
      ctx: Context
  ): Fu[JsObject] =
    if (apiVersion.exists(!_.puzzleV2))
      env.puzzle.jsonView.bc(puzzle = puzzle, user = newUser orElse ctx.me)
    else
      env.puzzle.jsonView(puzzle = puzzle, theme = theme.some, replay = replay, user = newUser orElse ctx.me)

  private def renderShow(
      puzzle: Puz,
      theme: PuzzleTheme,
      replay: Option[PuzzleReplay] = None
  )(implicit ctx: Context) =
    renderJson(puzzle, theme, replay) zip
      ctx.me.??(u => env.puzzle.session.getDifficulty(u) dmap some) map { case (json, difficulty) =>
        EnableSharedArrayBuffer(
          Ok(
            views.html.puzzle
              .show(puzzle, json, env.puzzle.jsonView.pref(ctx.pref), difficulty)
          )
        )
      }

  def daily =
    Open { implicit ctx =>
      NoBot {
        OptionFuResult(env.puzzle.daily.get) { daily =>
          negotiate(
            html = renderShow(daily.puzzle, PuzzleTheme.mix),
            api = v => renderJson(daily.puzzle, PuzzleTheme.mix, apiVersion = v.some) dmap { Ok(_) }
          ) map NoCache
        }
      }
    }

  def apiDaily =
    Action.async { implicit req =>
      env.puzzle.daily.get flatMap {
        _.fold(NotFound.fuccess) { daily =>
          JsonOk(env.puzzle.jsonView(daily.puzzle, none, none, none)(reqLang))
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
        Puz.toId(id) ?? { pid =>
          onComplete(env.puzzle.forms.round)(pid, PuzzleTheme findOrAny themeStr, mobileBc = false)
        }
      }
    }

  def mobileBcRound(nid: Long) =
    OpenBody { implicit ctx =>
      Puz.numericalId(nid) ?? {
        onComplete(env.puzzle.forms.bc.round)(_, PuzzleTheme.mix, mobileBc = true)
      }
    }

  def ofPlayer(name: Option[String], page: Int) =
    Open { implicit ctx =>
      val fixed = name.map(_.trim).filter(_.nonEmpty)
      fixed.??(env.user.repo.enabledNamed) orElse fuccess(ctx.me) flatMap { user =>
        user.?? { env.puzzle.api.puzzle.of(_, page) dmap some } map { puzzles =>
          Ok(views.html.puzzle.ofPlayer(~fixed, user, puzzles))
        }
      }
    }

  private def onComplete[A](form: Form[RoundData])(id: Puz.Id, theme: PuzzleTheme, mobileBc: Boolean)(implicit
      ctx: BodyContext[A]
  ) = {
    implicit val req = ctx.body
    form
      .bindFromRequest()
      .fold(
        jsonFormError,
        data =>
          {
            data.puzzleId match {
              case Some(streakNextId) =>
                env.puzzle.api.puzzle.find(streakNextId) flatMap {
                  case None => fuccess(Json.obj("streakComplete" -> true))
                  case Some(puzzle) =>
                    for {
                      score <- data.streakScore
                      if !data.result.win
                      if score > 0
                      _ = lila.mon.streak.run.score(ctx.isAuth).record(score)
                      userId <- ctx.userId
                    } {
                      lila.common.Bus.publish(lila.hub.actorApi.puzzle.StreakRun(userId, score), "streakRun")
                      env.user.repo.addStreakRun(userId, score)
                    }
                    renderJson(puzzle, theme) map { nextJson =>
                      Json.obj("next" -> nextJson)
                    }
                }
              case None =>
                lila.mon.puzzle.round.attempt(ctx.isAuth, theme.key.value).increment()
                ctx.me match {
                  case Some(me) =>
                    env.puzzle.finisher(id, theme.key, me, data.result) flatMap {
                      _ ?? { case (round, perf) =>
                        val newUser = me.copy(perfs = me.perfs.copy(puzzle = perf))
                        for {
                          _ <- env.puzzle.session.onComplete(round, theme.key)
                          json <-
                            if (mobileBc) fuccess {
                              env.puzzle.jsonView.bc.userJson(perf.intRating) ++ Json.obj(
                                "round" -> Json.obj(
                                  "ratingDiff" -> 0,
                                  "win"        -> data.result.win
                                ),
                                "voted" -> round.vote
                              )
                            }
                            else
                              data.replayDays match {
                                case None =>
                                  for {
                                    next     <- nextPuzzleForMe(theme.key)
                                    nextJson <- renderJson(next, theme, none, newUser.some)
                                  } yield Json.obj(
                                    "round" -> env.puzzle.jsonView.roundJson(me, round, perf),
                                    "next"  -> nextJson
                                  )
                                case Some(replayDays) =>
                                  for {
                                    _    <- env.puzzle.replay.onComplete(round, replayDays, theme.key)
                                    next <- env.puzzle.replay(me, replayDays, theme.key)
                                    json <- next match {
                                      case None => fuccess(Json.obj("replayComplete" -> true))
                                      case Some((puzzle, replay)) =>
                                        renderJson(puzzle, theme, replay.some) map { nextJson =>
                                          Json.obj(
                                            "round" -> env.puzzle.jsonView.roundJson(me, round, perf),
                                            "next"  -> nextJson
                                          )
                                        }
                                    }
                                  } yield json
                              }
                        } yield json
                      }
                    }
                  case None =>
                    env.puzzle.finisher.incPuzzlePlays(id)
                    if (mobileBc) fuccess(Json.obj("user" -> false))
                    else
                      nextPuzzleForMe(theme.key) flatMap {
                        renderJson(_, theme)
                      } map { json =>
                        Json.obj("next" -> json)
                      }
                }
            }
          } dmap JsonOk
      )
  }

  def streak =
    Open { implicit ctx =>
      NoBot {
        env.puzzle.streak.apply flatMap {
          _ ?? { case PuzzleStreak(ids, puzzle) =>
            env.puzzle.jsonView(puzzle = puzzle, PuzzleTheme.mix.some, none, user = ctx.me) map { preJson =>
              val json = preJson ++ Json.obj("streak" -> ids)
              EnableSharedArrayBuffer {
                NoCache {
                  Ok {
                    views.html.puzzle
                      .show(puzzle, json, env.puzzle.jsonView.pref(ctx.pref), none)
                  }
                }
              }
            }
          }
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
            vote => env.puzzle.api.vote.update(Puz.Id(id), me, vote) inject jsonOkResult
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
              vote => env.puzzle.api.theme.vote(me, Puz.Id(id), theme.key, vote) inject jsonOkResult
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
        case Some(theme) =>
          nextPuzzleForMe(theme.key) flatMap {
            renderShow(_, theme)
          }
        case None if themeOrId.size == Puz.idSize =>
          OptionFuResult(env.puzzle.api.puzzle find Puz.Id(themeOrId)) { puz =>
            ctx.me.?? { me =>
              !env.puzzle.api.round.exists(me, puz.id) map {
                _ ?? env.puzzle.api.casual.set(me, puz.id)
              }
            } >>
              renderShow(puz, PuzzleTheme.mix)
          }
        case None =>
          themeOrId.toLongOption
            .flatMap(Puz.numericalId.apply)
            .??(env.puzzle.api.puzzle.find) map {
            case None      => Redirect(routes.Puzzle.home)
            case Some(puz) => Redirect(routes.Puzzle.show(puz.id.value))
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

  def apiDashboard(days: Int) =
    Scoped(_.Puzzle.Read) { implicit req => me =>
      implicit val lang = reqLang
      JsonOptionOk {
        env.puzzle.dashboard(me, days) map2 { env.puzzle.jsonView.dashboardJson(_, days) }
      }
    }

  def dashboard(days: Int, path: String = "home") =
    Auth { implicit ctx => me =>
      get("u")
        .ifTrue(isGranted(_.Hunter))
        .??(env.user.repo.named)
        .map(_ | me)
        .flatMap { user =>
          env.puzzle.dashboard(user, days) map { dashboard =>
            path match {
              case "dashboard" => Ok(views.html.puzzle.dashboard.home(user, dashboard, days))
              case "improvementAreas" =>
                Ok(views.html.puzzle.dashboard.improvementAreas(user, dashboard, days))
              case "strengths" => Ok(views.html.puzzle.dashboard.strengths(user, dashboard, days))
              case _           => Redirect(routes.Puzzle.dashboard(days, "dashboard"))
            }
          }
        }
    }

  def replay(days: Int, themeKey: String) =
    Auth { implicit ctx => me =>
      val theme = PuzzleTheme.findOrAny(themeKey)
      env.puzzle.replay(me, days, theme.key) flatMap {
        case None                   => Redirect(routes.Puzzle.dashboard(days, "home")).fuccess
        case Some((puzzle, replay)) => renderShow(puzzle, theme, replay.some)
      }
    }

  def history(page: Int) =
    Auth { implicit ctx => me =>
      get("u")
        .ifTrue(isGranted(_.Hunter))
        .??(env.user.repo.named)
        .map(_ | me)
        .flatMap { user =>
          Reasonable(page) {
            env.puzzle.history(user, page) map { history =>
              Ok(views.html.puzzle.history(user, page, history))
            }
          }
        }
    }

  def mobileBcLoad(nid: Long) =
    Open { implicit ctx =>
      negotiate(
        html = notFound,
        _ =>
          OptionFuOk(Puz.numericalId(nid) ?? env.puzzle.api.puzzle.find) { puz =>
            env.puzzle.jsonView.bc(puzzle = puz, user = ctx.me)
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
            } dmap JsonOk
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
                  .flatMap { solution =>
                    Puz
                      .numericalId(solution.id)
                      .map(_ -> Result(solution.win))
                  }
                  .?? { case (id, solution) =>
                    env.puzzle.finisher(id, PuzzleTheme.mix.key, me, Result(solution.win))
                  } map {
                  case None => Ok(env.puzzle.jsonView.bc.userJson(me.perfs.puzzle.intRating))
                  case Some((round, perf)) =>
                    env.puzzle.session.onComplete(round, PuzzleTheme.mix.key)
                    Ok(env.puzzle.jsonView.bc.userJson(perf.intRating))
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
              intVote =>
                Puz.numericalId(nid) ?? {
                  env.puzzle.api.vote.update(_, me, intVote == 1) inject jsonOkResult
                }
            )
        }
      )
    }
}
