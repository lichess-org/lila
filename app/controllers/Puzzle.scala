package controllers

import chess.Color
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
import lila.user.{ User => UserModel }
import lila.puzzle.{
  Puzzle => Puz,
  PuzzleAngle,
  PuzzleDashboard,
  PuzzleDifficulty,
  PuzzleOpening,
  PuzzleReplay,
  PuzzleResult,
  PuzzleSettings,
  PuzzleStreak,
  PuzzleTheme
}
import play.api.mvc.Result

final class Puzzle(env: Env, apiC: => Api) extends LilaController(env) {

  private def renderJson(
      puzzle: Puz,
      angle: PuzzleAngle,
      replay: Option[PuzzleReplay] = None,
      newUser: Option[UserModel] = None,
      apiVersion: Option[ApiVersion] = None
  )(implicit
      ctx: Context
  ): Fu[JsObject] =
    if (apiVersion.exists(!_.puzzleV2))
      env.puzzle.jsonView.bc(puzzle = puzzle, user = newUser orElse ctx.me)
    else
      env.puzzle.jsonView(puzzle = puzzle, angle = angle.some, replay = replay, user = newUser orElse ctx.me)

  private def renderShow(
      puzzle: Puz,
      angle: PuzzleAngle,
      color: Option[Color] = None,
      replay: Option[PuzzleReplay] = None
  )(implicit ctx: Context) =
    renderJson(puzzle, angle, replay) zip
      ctx.me.??(u => env.puzzle.session.getSettings(u) dmap some) map { case (json, settings) =>
        EnableSharedArrayBuffer(
          Ok(
            views.html.puzzle
              .show(
                puzzle,
                json,
                env.puzzle.jsonView.pref(ctx.pref),
                settings | PuzzleSettings.default(color)
              )
          )
        )
      }

  def daily =
    Open { implicit ctx =>
      NoBot {
        OptionFuResult(env.puzzle.daily.get) { daily =>
          negotiate(
            html = renderShow(daily.puzzle, PuzzleAngle.mix),
            api = v => renderJson(daily.puzzle, PuzzleAngle.mix, apiVersion = v.some) dmap { Ok(_) }
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
        val angle = PuzzleAngle.mix
        nextPuzzleForMe(angle, none) flatMap {
          renderShow(_, angle)
        }
      }
    }

  private def nextPuzzleForMe(angle: PuzzleAngle, color: Option[Option[Color]])(implicit
      ctx: Context
  ): Fu[Puz] =
    ctx.me match {
      case Some(me) =>
        color.?? { colorChoice =>
          env.puzzle.session.setAngleAndColor(me, angle, colorChoice)
        } >> env.puzzle.selector.nextPuzzleFor(me, angle)
      case None => env.puzzle.anon.getOneFor(angle, ~color) orFail "Couldn't find a puzzle for anon!"
    }

  def complete(angleStr: String, id: String) =
    OpenBody { implicit ctx =>
      NoBot {
        Puz.toId(id) ?? { pid =>
          onComplete(env.puzzle.forms.round)(pid, PuzzleAngle findOrMix angleStr, mobileBc = false)
        }
      }
    }

  def mobileBcRound(nid: Long) =
    OpenBody { implicit ctx =>
      Puz.numericalId(nid) ?? {
        onComplete(env.puzzle.forms.bc.round)(_, PuzzleAngle.mix, mobileBc = true)
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

  private def onComplete[A](form: Form[RoundData])(id: Puz.Id, angle: PuzzleAngle, mobileBc: Boolean)(implicit
      ctx: BodyContext[A]
  ) = {
    implicit val req = ctx.body
    form
      .bindFromRequest()
      .fold(
        jsonFormError,
        data =>
          {
            data.streakPuzzleId match {
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
                    renderJson(puzzle, angle) map { nextJson =>
                      Json.obj("next" -> nextJson)
                    }
                }
              case None =>
                lila.mon.puzzle.round.attempt(ctx.isAuth, angle.key, data.rated).increment()
                ctx.me match {
                  case Some(me) =>
                    env.puzzle.finisher(id, angle, me, data.result, data.mode) flatMap {
                      _ ?? { case (round, perf) =>
                        val newUser = me.copy(perfs = me.perfs.copy(puzzle = perf))
                        for {
                          _ <- env.puzzle.session.onComplete(round, angle)
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
                              (data.replayDays, angle.asTheme) match {
                                case (Some(replayDays), Some(theme)) =>
                                  for {
                                    _    <- env.puzzle.replay.onComplete(round, replayDays, angle)
                                    next <- env.puzzle.replay(me, replayDays.some, theme)
                                    json <- next match {
                                      case None => fuccess(Json.obj("replayComplete" -> true))
                                      case Some((puzzle, replay)) =>
                                        renderJson(puzzle, angle, replay.some) map { nextJson =>
                                          Json.obj(
                                            "round" -> env.puzzle.jsonView.roundJson(me, round, perf),
                                            "next"  -> nextJson
                                          )
                                        }
                                    }
                                  } yield json
                                case _ =>
                                  for {
                                    next     <- nextPuzzleForMe(angle, none)
                                    nextJson <- renderJson(next, angle, none, newUser.some)
                                  } yield Json.obj(
                                    "round" -> env.puzzle.jsonView.roundJson(me, round, perf),
                                    "next"  -> nextJson
                                  )
                              }
                        } yield json
                      }
                    }
                  case None =>
                    env.puzzle.finisher.incPuzzlePlays(id)
                    if (mobileBc) fuccess(Json.obj("user" -> false))
                    else
                      nextPuzzleForMe(angle, data.color map some) flatMap {
                        renderJson(_, angle)
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
            env.puzzle.jsonView(puzzle = puzzle, PuzzleAngle.mix.some, none, user = ctx.me) map { preJson =>
              val json = preJson ++ Json.obj("streak" -> ids)
              EnableSharedArrayBuffer {
                NoCache {
                  Ok {
                    views.html.puzzle
                      .show(puzzle, json, env.puzzle.jsonView.pref(ctx.pref), PuzzleSettings.default)
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
    env.puzzle.api.angles map { all =>
      Ok(views.html.puzzle.theme.list(all))
    }
  }

  def openings(order: String) = Open { implicit ctx =>
    env.puzzle.opening.collection flatMap { collection =>
      ctx.me.?? { me =>
        env.insight.insightUserApi.find(me.id) map {
          _ ?? { insightUser =>
            collection.makeMine(insightUser.families, insightUser.openings).some
          }
        }
      } map { mine =>
        Ok(views.html.puzzle.opening.all(collection, mine, PuzzleOpening.Order(order)))
      }
    }
  }

  def show(angleOrId: String) = Open { implicit ctx =>
    NoBot {
      PuzzleAngle find angleOrId match {
        case Some(angle) =>
          nextPuzzleForMe(angle, none) flatMap {
            renderShow(_, angle)
          }
        case _ if angleOrId.size == Puz.idSize =>
          OptionFuResult(env.puzzle.api.puzzle find Puz.Id(angleOrId)) { puzzle =>
            ctx.me.?? { env.puzzle.api.casual.setCasualIfNotYetPlayed(_, puzzle) } >>
              renderShow(puzzle, PuzzleAngle.mix)
          }
        case _ =>
          angleOrId.toLongOption
            .flatMap(Puz.numericalId.apply)
            .??(env.puzzle.api.puzzle.find) map {
            case None      => Redirect(routes.Puzzle.home)
            case Some(puz) => Redirect(routes.Puzzle.show(puz.id.value))
          }
      }
    }
  }

  def showWithAngle(angleKey: String, id: String) = Open { implicit ctx =>
    NoBot {
      val angle = PuzzleAngle.findOrMix(angleKey)
      OptionFuResult(env.puzzle.api.puzzle find Puz.Id(id)) { puzzle =>
        if (angle.asTheme.exists(theme => !puzzle.themes.contains(theme)))
          Redirect(routes.Puzzle.show(puzzle.id.value)).fuccess
        else
          ctx.me.?? { env.puzzle.api.casual.setCasualIfNotYetPlayed(_, puzzle) } >>
            renderShow(puzzle, angle)
      }
    }
  }

  def angleAndColor(angleKey: String, colorKey: String) = Open { implicit ctx =>
    NoBot {
      PuzzleAngle.find(angleKey).fold(Redirect(routes.Puzzle.openings()).fuccess) { angle =>
        val color = Color fromName colorKey
        nextPuzzleForMe(angle, color.some) flatMap {
          renderShow(_, angle, color = color)
        }
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

  def apiDashboard(days: Int) = {
    def render(me: UserModel)(implicit lang: play.api.i18n.Lang) = JsonOptionOk {
      env.puzzle.dashboard(me, days) map2 { env.puzzle.jsonView.dashboardJson(_, days) }
    }
    AuthOrScoped(_.Puzzle.Read)(
      auth = ctx => me => render(me)(ctx.lang),
      scoped = req => me => render(me)(reqLang(req))
    )
  }

  def dashboard(days: Int, path: String = "home", u: Option[String]) =
    DashboardPage(u) { implicit ctx => user =>
      env.puzzle.dashboard(user, days) map { dashboard =>
        path match {
          case "dashboard" => Ok(views.html.puzzle.dashboard.home(user, dashboard, days))
          case "improvementAreas" =>
            Ok(views.html.puzzle.dashboard.improvementAreas(user, dashboard, days))
          case "strengths" => Ok(views.html.puzzle.dashboard.strengths(user, dashboard, days))
          case _ => Redirect(routes.Puzzle.dashboard(days, "dashboard", !ctx.is(user) option user.username))
        }
      }
    }

  def replay(days: Int, themeKey: String) =
    Auth { implicit ctx => me =>
      val theme         = PuzzleTheme.findOrMix(themeKey)
      val checkedDayOpt = PuzzleDashboard.getclosestDay(days)
      env.puzzle.replay(me, checkedDayOpt, theme.key) flatMap {
        case None =>
          Redirect(routes.Puzzle.dashboard(days, "home", none)).fuccess
        case Some((puzzle, replay)) => renderShow(puzzle, PuzzleAngle(theme), replay = replay.some)
      }
    }

  def mobileHistory(page: Int) =
    Auth { implicit ctx => me =>
      negotiate(
        html = notFound,
        _ => {
          import lila.puzzle.JsonView._
          Reasonable(page) {
            env.puzzle.history(me, page) map { historyPaginator =>
              Ok(lila.common.paginator.PaginatorJson(historyPaginator))
            }
          }
        }
      )

    }

  def history(page: Int, u: Option[String]) =
    DashboardPage(u) { implicit ctx => user =>
      Reasonable(page) {
        env.puzzle.history(user, page) map { history =>
          Ok(views.html.puzzle.history(user, page, history))
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
            val angle = PuzzleAngle.mix
            nextPuzzleForMe(angle, none) flatMap { puzzle =>
              renderJson(puzzle, angle, apiVersion = v.some)
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
                      .map(_ -> PuzzleResult(solution.win))
                  }
                  .?? { case (id, solution) =>
                    env.puzzle.finisher(id, PuzzleAngle.mix, me, PuzzleResult(solution.win), chess.Mode.Rated)
                  } map {
                  case None => Ok(env.puzzle.jsonView.bc.userJson(me.perfs.puzzle.intRating))
                  case Some((round, perf)) =>
                    env.puzzle.session.onComplete(round, PuzzleAngle.mix)
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

  def help =
    Open { implicit ctx =>
      Ok(html.site.helpModal.puzzle).fuccess
    }

  private def DashboardPage(username: Option[String])(f: Context => UserModel => Fu[Result]) =
    Auth { implicit ctx => me =>
      username
        .??(env.user.repo.named)
        .flatMap {
          _ ?? { user =>
            (fuccess(isGranted(_.CheatHunter)) >>|
              (user.enabled ?? env.clas.api.clas.isTeacherOf(me.id, user.id))) map {
              _ option user
            }
          }
        }
        .dmap(_ | me)
        .flatMap(f(ctx))
    }
}
