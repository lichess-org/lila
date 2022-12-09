package controllers

import chess.Color
import play.api.data.Form
import play.api.libs.json.*
import play.api.mvc.Result
import scala.util.chaining.*
import views.*

import lila.api.BodyContext
import lila.api.Context
import lila.app.{ given, * }
import lila.common.ApiVersion
import lila.common.Json.given
import lila.common.config.MaxPerSecond
import lila.i18n.I18nLangPicker
import lila.puzzle.PuzzleForm.RoundData
import lila.puzzle.{ Puzzle as Puz, PuzzleAngle, PuzzleSettings, PuzzleStreak, PuzzleTheme }
import lila.user.{ User as UserModel }
import lila.common.LangPath

final class Puzzle(env: Env, apiC: => Api) extends LilaController(env):

  private def renderJson(
      puzzle: Puz,
      angle: PuzzleAngle,
      replay: Option[lila.puzzle.PuzzleReplay] = None,
      newUser: Option[UserModel] = None,
      apiVersion: Option[ApiVersion] = None
  )(using
      ctx: Context
  ): Fu[JsObject] =
    if (apiVersion.exists(v => !ApiVersion.puzzleV2(v)))
      env.puzzle.jsonView.bc(puzzle = puzzle, user = newUser orElse ctx.me)
    else
      env.puzzle.jsonView(
        puzzle = puzzle,
        angle = angle.some,
        replay = replay,
        user = newUser orElse ctx.me
      )

  private def renderShow(
      puzzle: Puz,
      angle: PuzzleAngle,
      color: Option[Color] = None,
      replay: Option[lila.puzzle.PuzzleReplay] = None,
      langPath: Option[LangPath] = None
  )(implicit ctx: Context) =
    renderJson(puzzle, angle, replay) zip
      ctx.me.??(u => env.puzzle.session.getSettings(u) dmap some) map { case (json, settings) =>
        Ok(
          views.html.puzzle
            .show(
              puzzle,
              json,
              env.puzzle.jsonView.pref(ctx.pref),
              settings | PuzzleSettings.default(color),
              langPath
            )
        ).enableSharedArrayBuffer
      }

  def daily =
    Open { implicit ctx =>
      NoBot {
        OptionFuResult(env.puzzle.daily.get) { daily =>
          negotiate(
            html = renderShow(daily.puzzle, PuzzleAngle.mix),
            api = v => renderJson(daily.puzzle, PuzzleAngle.mix, apiVersion = v.some) dmap { Ok(_) }
          ) dmap (_.noCache)
        }
      }
    }

  def apiDaily =
    Action.async { implicit req =>
      env.puzzle.daily.get flatMap {
        _.fold(notFoundJson()) { daily =>
          JsonOk(env.puzzle.jsonView(daily.puzzle, none, none, none)(using reqLang))
        }
      }
    }

  def apiShow(id: PuzzleId) =
    Action.async { implicit req =>
      env.puzzle.api.puzzle find id flatMap {
        _.fold(notFoundJson()) { puzzle =>
          JsonOk(env.puzzle.jsonView(puzzle, none, none, none)(using reqLang))
        }
      }
    }

  def home = Open(serveHome(_))

  def homeLang = LangPage(routes.Puzzle.home.url)(serveHome(_))

  private def serveHome(implicit ctx: Context) =
    NoBot {
      val angle = PuzzleAngle.mix
      nextPuzzleForMe(angle, none) flatMap {
        renderShow(_, angle, langPath = LangPath(routes.Puzzle.home).some)
      }
    }

  private def nextPuzzleForMe(angle: PuzzleAngle, color: Option[Option[Color]])(using
      ctx: Context
  ): Fu[Puz] =
    ctx.me match
      case Some(me) =>
        color.?? { colorChoice =>
          env.puzzle.session.setAngleAndColor(me, angle, colorChoice)
        } >> env.puzzle.selector.nextPuzzleFor(me, angle)
      case None => env.puzzle.anon.getOneFor(angle, ~color) orFail "Couldn't find a puzzle for anon!"

  def complete(angleStr: String, id: PuzzleId) =
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

  def ofPlayer(name: Option[UserStr], page: Int) =
    Open { implicit ctx =>
      val userId = name flatMap lila.user.User.validateId
      userId.??(env.user.repo.enabledById) orElse fuccess(ctx.me) flatMap { user =>
        user.?? { env.puzzle.api.puzzle.of(_, page) dmap some } map { puzzles =>
          Ok(views.html.puzzle.ofPlayer(name.??(_.value), user, puzzles))
        }
      }
    }

  private def onComplete[A](form: Form[RoundData])(id: PuzzleId, angle: PuzzleAngle, mobileBc: Boolean)(using
      ctx: BodyContext[A]
  ) =
    given play.api.mvc.Request[?] = ctx.body
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
                      if data.win.no
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
                    env.puzzle.finisher(id, angle, me, data.win, data.mode) flatMap {
                      _ ?? { case (round, perf) =>
                        val newUser = me.copy(perfs = me.perfs.copy(puzzle = perf))
                        for {
                          _ <- env.puzzle.session.onComplete(round, angle)
                          json <-
                            if (mobileBc) fuccess {
                              env.puzzle.jsonView.bc.userJson(perf.intRating) ++ Json.obj(
                                "round" -> Json.obj(
                                  "ratingDiff" -> 0,
                                  "win"        -> data.win
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

  def streak     = Open(serveStreak(_))
  def streakLang = LangPage(routes.Puzzle.streak)(serveStreak(_))
  private def serveStreak(implicit ctx: Context) = NoBot {
    env.puzzle.streak.apply flatMap {
      _ ?? { case PuzzleStreak(ids, puzzle) =>
        env.puzzle.jsonView(puzzle = puzzle, PuzzleAngle.mix.some, none, user = ctx.me) map { preJson =>
          val json = preJson ++ Json.obj("streak" -> ids)
          Ok(
            views.html.puzzle
              .show(
                puzzle,
                json,
                env.puzzle.jsonView.pref(ctx.pref),
                PuzzleSettings.default,
                langPath = LangPath(routes.Puzzle.streak).some
              )
          ).noCache.enableSharedArrayBuffer
        }
      }
    }
  }

  def vote(id: PuzzleId) =
    AuthBody { implicit ctx => me =>
      NoBot {
        given play.api.mvc.Request[?] = ctx.body
        env.puzzle.forms.vote
          .bindFromRequest()
          .fold(
            jsonFormError,
            vote => env.puzzle.api.vote.update(id, me, vote) inject jsonOkResult
          )
      }
    }

  def voteTheme(id: PuzzleId, themeStr: String) =
    AuthBody { implicit ctx => me =>
      NoBot {
        PuzzleTheme.findDynamic(themeStr) ?? { theme =>
          given play.api.mvc.Request[?] = ctx.body
          env.puzzle.forms.themeVote
            .bindFromRequest()
            .fold(
              jsonFormError,
              vote => env.puzzle.api.theme.vote(me, id, theme.key, vote) inject jsonOkResult
            )
        }
      }
    }

  def setDifficulty(theme: String) =
    AuthBody { implicit ctx => me =>
      NoBot {
        given play.api.mvc.Request[?] = ctx.body
        env.puzzle.forms.difficulty
          .bindFromRequest()
          .fold(
            jsonFormError,
            diff =>
              lila.puzzle.PuzzleDifficulty.find(diff) ?? { env.puzzle.session.setDifficulty(me, _) } inject
                Redirect(routes.Puzzle.show(theme))
          )
      }
    }

  def themes = Open(serveThemes(_))

  def themesLang = LangPage(routes.Puzzle.themes)(serveThemes(_))

  private def serveThemes(implicit ctx: Context) =
    env.puzzle.api.angles map { all =>
      Ok(views.html.puzzle.theme.list(all))
    }

  def openings(order: String) = Open { implicit ctx =>
    env.puzzle.opening.collection flatMap { collection =>
      ctx.me.?? { me =>
        env.insight.api.insightUser(me) map {
          _.some.filterNot(_.isEmpty) ?? { insightUser =>
            collection.makeMine(insightUser.families, insightUser.openings).some
          }
        }
      } map { mine =>
        Ok(views.html.puzzle.opening.all(collection, mine, lila.puzzle.PuzzleOpening.Order(order)))
      }
    }
  }

  def show(angleOrId: String) = Open(serveShow(angleOrId)(_))
  def showLang(lang: String, angleOrId: String) =
    LangPage(routes.Puzzle.show(angleOrId).url)(serveShow(angleOrId)(_))(lang)

  private def serveShow(angleOrId: String)(implicit ctx: Context) =
    NoBot {
      val langPath = LangPath(routes.Puzzle.show(angleOrId)).some
      PuzzleAngle find angleOrId match
        case Some(angle) =>
          nextPuzzleForMe(angle, none) flatMap {
            renderShow(_, angle, langPath = langPath)
          }
        case _ =>
          lila.puzzle.Puzzle toId angleOrId match
            case Some(id) =>
              OptionFuResult(env.puzzle.api.puzzle find id) { puzzle =>
                ctx.me.?? { env.puzzle.api.casual.setCasualIfNotYetPlayed(_, puzzle) } >>
                  renderShow(puzzle, PuzzleAngle.mix, langPath = langPath)
              }
            case _ =>
              angleOrId.toLongOption
                .flatMap(Puz.numericalId.apply)
                .??(env.puzzle.api.puzzle.find) map {
                case None      => Redirect(routes.Puzzle.home)
                case Some(puz) => Redirect(routes.Puzzle.show(puz.id))
              }
    }

  def showWithAngle(angleKey: String, id: PuzzleId) = Open { implicit ctx =>
    NoBot {
      val angle = PuzzleAngle.findOrMix(angleKey)
      OptionFuResult(env.puzzle.api.puzzle find id) { puzzle =>
        if (angle.asTheme.exists(theme => !puzzle.themes.contains(theme)))
          Redirect(routes.Puzzle.show(puzzle.id)).toFuccess
        else
          ctx.me.?? { env.puzzle.api.casual.setCasualIfNotYetPlayed(_, puzzle) } >>
            renderShow(puzzle, angle)
      }
    }
  }

  def angleAndColor(angleKey: String, colorKey: String) = Open { implicit ctx =>
    NoBot {
      PuzzleAngle.find(angleKey).fold(Redirect(routes.Puzzle.openings()).toFuccess) { angle =>
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
        .toFuccess
    }

  def apiDashboard(days: Int) =
    def render(me: UserModel)(implicit lang: play.api.i18n.Lang) = JsonOptionOk {
      env.puzzle.dashboard(me, days) map2 { env.puzzle.jsonView.dashboardJson(_, days) }
    }
    AuthOrScoped(_.Puzzle.Read)(
      auth = ctx => me => render(me)(ctx.lang),
      scoped = req => me => render(me)(reqLang(req))
    )

  def dashboard(days: Int, path: String = "home", u: Option[UserStr]) =
    DashboardPage(u) { implicit ctx => user =>
      env.puzzle.dashboard(user, days) map { dashboard =>
        path match
          case "dashboard" => Ok(views.html.puzzle.dashboard.home(user, dashboard, days))
          case "improvementAreas" =>
            Ok(views.html.puzzle.dashboard.improvementAreas(user, dashboard, days))
          case "strengths" => Ok(views.html.puzzle.dashboard.strengths(user, dashboard, days))
          case _ => Redirect(routes.Puzzle.dashboard(days, "dashboard", !ctx.is(user) option user.username))
      }
    }

  def replay(days: Int, themeKey: PuzzleTheme.Key) =
    Auth { implicit ctx => me =>
      val theme         = PuzzleTheme.findOrMix(themeKey)
      val checkedDayOpt = lila.puzzle.PuzzleDashboard.getClosestDay(days)
      env.puzzle.replay(me, checkedDayOpt, theme.key) flatMap {
        case None =>
          Redirect(routes.Puzzle.dashboard(days, "home", none)).toFuccess
        case Some((puzzle, replay)) => renderShow(puzzle, PuzzleAngle(theme), replay = replay.some)
      }
    }

  def mobileHistory(page: Int) =
    Auth { implicit ctx => me =>
      negotiate(
        html = notFound,
        _ => {
          import lila.puzzle.JsonView.given
          Reasonable(page) {
            env.puzzle.history(me, page) map { historyPaginator =>
              Ok(lila.common.paginator.PaginatorJson(historyPaginator))
            }
          }
        }
      )

    }

  def history(page: Int, u: Option[UserStr]) =
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
  def mobileBcBatchSelect = Auth { implicit ctx => me =>
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
  def mobileBcBatchSolve = AuthBody(parse.json) { implicit ctx => me =>
    negotiate(
      html = notFound,
      api = v => {
        import lila.puzzle.PuzzleForm.bc.*
        import lila.puzzle.PuzzleWin
        ctx.body.body
          .validate[SolveData]
          .fold(
            err => BadRequest(err.toString).toFuccess,
            data =>
              data.solutions.lastOption
                .flatMap { solution =>
                  Puz
                    .numericalId(solution.id)
                    .map(_ -> PuzzleWin(solution.win))
                }
                .?? { (id, solution) =>
                  env.puzzle.finisher(id, PuzzleAngle.mix, me, solution, chess.Mode.Rated)
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
          given play.api.mvc.Request[?] = ctx.body
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
      Ok(html.site.keyboardHelpModal.puzzle).toFuccess
    }

  private def DashboardPage(username: Option[UserStr])(f: Context => UserModel => Fu[Result]) =
    Auth { implicit ctx => me =>
      username
        .??(env.user.repo.byId)
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
