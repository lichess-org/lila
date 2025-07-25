package controllers

import play.api.data.Form
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.core.i18n.Language
import lila.core.id.PuzzleId
import lila.puzzle.{
  Puzzle as Puz,
  PuzzleAngle,
  PuzzleDifficulty,
  PuzzleForm,
  PuzzleSettings,
  PuzzleStreak,
  PuzzleTheme,
  difficultyCookie
}
import lila.rating.PerfType
import lila.ui.LangPath
import scalalib.model.Days

final class Puzzle(env: Env, apiC: => Api) extends LilaController(env):

  import env.puzzle.{ jsonView, selector }

  private def renderShow(
      puzzle: Puz,
      angle: PuzzleAngle,
      color: Option[Color] = None,
      replay: Option[lila.puzzle.PuzzleReplay] = None,
      langPath: Option[LangPath] = None
  )(using ctx: Context)(using Perf) = for
    json <- jsonView.analysis(puzzle, angle, replay)
    settings <- ctx.user.soFu(env.puzzle.session.getSettings)
    prefJson = jsonView.pref(ctx.pref)
    page <- renderPage:
      views.puzzle.ui.show(puzzle, json, prefJson, settings | PuzzleSettings.default(color), langPath)
  yield Ok(page).enforceCrossSiteIsolation

  def daily = Open:
    NoBot:
      Found(env.puzzle.daily.get): daily =>
        WithPuzzlePerf:
          negotiateApi(
            html = renderShow(daily.puzzle, PuzzleAngle.mix),
            api = v => jsonView.analysis(daily.puzzle, PuzzleAngle.mix, apiVersion = v.some).dmap { Ok(_) }
          ).dmap(_.noCache)

  def apiDaily = Anon:
    Found(env.puzzle.daily.get): daily =>
      WithPuzzlePerf:
        JsonOk(env.puzzle.jsonView(daily.puzzle, none, none))

  def apiShow(id: PuzzleId) = Anon:
    Found(env.puzzle.api.puzzle.find(id)): puzzle =>
      WithPuzzlePerf:
        JsonOk(env.puzzle.jsonView(puzzle, none, none))

  def home = Open(serveHome)

  def homeLang = LangPage(routes.Puzzle.home.url)(serveHome)

  private def serveHome(using Context) = NoBot:
    val angle = PuzzleAngle.mix
    WithPuzzlePerf:
      selector
        .nextPuzzleFor(angle, none, PuzzleDifficulty.fromReqSession(req))
        .flatMap:
          _.fold(redirectNoPuzzle):
            renderShow(_, angle, langPath = LangPath(routes.Puzzle.home).some)

  private def redirectNoPuzzle: Fu[Result] =
    Redirect(routes.Puzzle.themes).flashFailure("No more puzzles available! Try another theme.")

  def complete(angleStr: String, id: PuzzleId) = OpenBody:
    NoBot:
      onComplete(env.puzzle.forms.round)(id, PuzzleAngle.findOrMix(angleStr), mobileBc = false)

  def mobileBcRound(nid: Long) = OpenBody:
    Puz
      .numericalId(nid)
      .so:
        onComplete(env.puzzle.forms.bc.round)(_, PuzzleAngle.mix, mobileBc = true)

  private def onComplete[A](
      form: Form[PuzzleForm.RoundData]
  )(id: PuzzleId, angle: PuzzleAngle, mobileBc: Boolean)(using ctx: BodyContext[A]): Fu[Result] =
    bindForm(form)(
      doubleJsonFormError,
      data =>
        WithPuzzlePerf:
          JsonOk(env.puzzle.complete.onComplete(data)(id, angle, mobileBc))
    )

  def ofPlayer(name: Option[UserStr], page: Int) = Open:
    val userId = name.flatMap(_.validateId)
    for
      user <- userId.so(env.user.repo.enabledById).orElse(fuccess(ctx.me.map(_.value)))
      puzzles <- user.soFu(env.puzzle.api.puzzle.of(_, page))
      page <- renderPage(views.puzzle.ui.ofPlayer(name.so(_.value), user, puzzles))
    yield Ok(page)

  def streak = Open(serveStreak)
  def streakLang = LangPage(routes.Puzzle.streak)(serveStreak)

  private def serveStreak(using ctx: Context) = NoBot:
    FoundPage(streakJsonAndPuzzle): (json, puzzle) =>
      val prefJson = env.puzzle.jsonView.pref(ctx.pref)
      val langPath = LangPath(routes.Puzzle.streak).some
      views.puzzle.ui.show(puzzle, json, prefJson, PuzzleSettings.default, langPath)
    .map(_.noCache.enforceCrossSiteIsolation)

  private def streakJsonAndPuzzle(using Context) =
    given Perf = lila.rating.Perf.default
    env.puzzle.streak.apply.flatMapz { case PuzzleStreak(ids, puzzle) =>
      env.puzzle.jsonView(puzzle = puzzle, PuzzleAngle.mix.some, none).map { puzzleJson =>
        (puzzleJson ++ Json.obj("streak" -> ids), puzzle).some
      }
    }

  def apiStreak = Anon:
    streakJsonAndPuzzle.orNotFound: (json, _) =>
      JsonOk(json)

  def apiStreakResult(score: Int) = ScopedBody(_.Puzzle.Write, _.Web.Mobile) { _ ?=> me ?=>
    if score > 0 && score < PuzzleForm.maxStreakScore then
      lila.mon.streak.run.score("mobile").record(score)
      env.puzzle.complete.setStreakResult(me, score)
      NoContent
    else BadRequest
  }

  def vote(id: PuzzleId) = AuthBody { _ ?=> me ?=>
    NoBot:
      bindForm(env.puzzle.forms.vote)(
        doubleJsonFormError,
        vote => env.puzzle.api.vote.update(id, me, vote).inject(jsonOkResult)
      )
  }

  def report(id: PuzzleId) = AuthBody { _ ?=> me ?=>
    NoBot:
      bindForm(env.puzzle.forms.report)(
        badJsonFormError,
        reportText =>
          env.puzzle.api.puzzle
            .reportDedup(id)
            .so(env.irc.api.reportPuzzle(me.light, id, reportText))
            .inject(jsonOkResult)
      )
  }

  def voteTheme(id: PuzzleId, themeStr: String) = AuthBody { _ ?=> me ?=>
    NoBot:
      PuzzleTheme
        .findDynamic(themeStr)
        .so: theme =>
          bindForm(env.puzzle.forms.themeVote)(
            doubleJsonFormError,
            vote => env.puzzle.api.theme.vote(me, id, theme.key, vote).inject(jsonOkResult)
          )
  }

  def setDifficulty(theme: String) = AuthBody { _ ?=> me ?=>
    NoBot:
      bindForm(env.puzzle.forms.difficulty)(
        doubleJsonFormError,
        diff =>
          WithPuzzlePerf:
            PuzzleDifficulty
              .find(diff)
              .so(env.puzzle.session.setDifficulty)
              .inject:
                Redirect(routes.Puzzle.show(theme))
                  .withCookies(env.security.lilaCookie.session(difficultyCookie, diff))
      )
  }

  def themes = Open(serveThemes)
  def themesLang = LangPage(routes.Puzzle.themes)(serveThemes)

  private def serveThemes(using Context) =
    env.puzzle.api.angles.flatMap: angles =>
      negotiate(
        html = Ok.page(views.puzzle.ui.themes(angles)),
        json = Ok(lila.puzzle.JsonView.angles(angles))
      )

  def openings(order: String) = Open:
    env.puzzle.opening.collection.flatMap: collection =>
      negotiate(
        html = for
          insights <- ctx.me.so(env.insight.api.insightUser(_).dmap(_.some.filterNot(_.isEmpty)))
          myOpenings = insights.map(u => collection.makeMine(u.families, u.openings))
          page = views.puzzle.ui.opening.all(collection, myOpenings, lila.puzzle.PuzzleOpening.Order(order))
          result <- Ok.page(page)
        yield result,
        json = Ok(lila.puzzle.JsonView.openings(collection))
      )

  def show(angleOrId: String) = Open(serveShow(angleOrId))
  def showLang(language: Language, angleOrId: String) =
    LangPage(routes.Puzzle.show(angleOrId).url)(serveShow(angleOrId))(language)

  private def serveShow(angleOrId: String)(using ctx: Context) = NoBot:
    val langPath = LangPath(routes.Puzzle.show(angleOrId)).some
    WithPuzzlePerf:
      PuzzleAngle.find(angleOrId) match
        case Some(angle) =>
          selector
            .nextPuzzleFor(angle, none, PuzzleDifficulty.fromReqSession(req))
            .flatMap:
              _.fold(redirectNoPuzzle) { renderShow(_, angle, langPath = langPath) }
        case _ =>
          lila.puzzle.Puzzle.toId(angleOrId) match
            case Some(id) =>
              Found(env.puzzle.api.puzzle.find(id)): puzzle =>
                ctx.me.so { env.puzzle.api.casual.setCasualIfNotYetPlayed(_, puzzle) } >>
                  renderShow(puzzle, PuzzleAngle.mix, langPath = langPath)
            case _ =>
              angleOrId.toLongOption
                .flatMap(Puz.numericalId.apply)
                .so(env.puzzle.api.puzzle.find)
                .map:
                  case None => Redirect(routes.Puzzle.home)
                  case Some(puz) => Redirect(routes.Puzzle.show(puz.id.value))

  def showWithAngle(angleKey: String, id: PuzzleId) = Open:
    NoBot:
      val angle = PuzzleAngle.findOrMix(angleKey)
      Found(env.puzzle.api.puzzle.find(id)): puzzle =>
        if angle.asTheme.exists(theme => !puzzle.themes.contains(theme))
        then Redirect(routes.Puzzle.show(puzzle.id.value))
        else
          WithPuzzlePerf:
            for
              _ <- ctx.me.so(env.puzzle.api.casual.setCasualIfNotYetPlayed(_, puzzle))
              res <- renderShow(puzzle, angle)
            yield res

  def angleAndColor(angleKey: String, colorKey: String) = Open:
    NoBot:
      PuzzleAngle
        .find(angleKey)
        .fold(Redirect(routes.Puzzle.openings()).toFuccess): angle =>
          val color = Color.fromName(colorKey)
          WithPuzzlePerf:
            selector
              .nextPuzzleFor(angle, color.some, PuzzleDifficulty.fromReqSession(req))
              .flatMap:
                _.fold(redirectNoPuzzle) { renderShow(_, angle, color = color) }

  def apiNext = AnonOrScoped(_.Puzzle.Read):
    WithPuzzlePerf:
      validateParam("angle", PuzzleAngle.find): angle =>
        validateParam("difficulty", PuzzleDifficulty.find): difficulty =>
          FoundOk(selector.nextPuzzleFor(angle | PuzzleAngle.mix, none, difficulty)):
            env.puzzle.jsonView(_, none, none)

  private def validateParam[A](name: String, read: String => Option[A])(
      f: Option[A] => Fu[Result]
  )(using Context): Fu[Result] =
    get(name).fold(f(none)): param =>
      read(param).fold(BadRequest(s"Invalid $name=$param").toFuccess)(p => f(p.some))

  def frame = Anon:
    InEmbedContext:
      env.puzzle.daily.get.flatMap:
        _.fold(InternalServerError("No daily puzzle yet").toFuccess): p =>
          Ok.snip(views.puzzle.embed(p))

  def activity = Scoped(_.Puzzle.Read, _.Web.Mobile) { ctx ?=> me ?=>
    val config = lila.puzzle.PuzzleActivity.Config(
      user = me,
      max = getIntAs[Max]("max").map(_.atLeast(1)),
      before = getTimestamp("before"),
      since = getTimestamp("since")
    )
    apiC.GlobalConcurrencyLimitPerIpAndUserOption(me.some)(env.puzzle.activity.stream(config))(jsToNdJson)
  }

  def apiDashboard(days: Days) = AuthOrScoped(_.Puzzle.Read, _.Web.Mobile) { _ ?=> me ?=>
    JsonOptionOk:
      env.puzzle.dashboard(me, days).map2 { env.puzzle.jsonView.dashboardJson(_, days) }
  }

  def dashboard(days: Days, path: String = "home", u: Option[UserStr]) =
    DashboardPage(u) { ctx ?=> user =>
      env.puzzle.dashboard(user, days).flatMap { dashboard =>
        path match
          case "dashboard" => Ok.page(views.puzzle.dashboard.home(user, dashboard, days))
          case "improvementAreas" =>
            Ok.page(views.puzzle.dashboard.improvementAreas(user, dashboard, days))
          case "strengths" => Ok.page(views.puzzle.dashboard.strengths(user, dashboard, days))
          case _ =>
            Redirect(routes.Puzzle.dashboard(days, "dashboard", (ctx.isnt(user)).option(user.username)))
      }
    }

  def replay(days: Days, themeKey: String) = Auth { ctx ?=> me ?=>
    replayOf(days, themeKey).flatMap:
      case None => Redirect(routes.Puzzle.dashboard(days, "home", none))
      case Some((puzzle, replay), angle) => WithPuzzlePerf(renderShow(puzzle, angle, replay = replay.some))
  }

  def apiReplay(days: Days, themeKey: String) = Scoped(_.Puzzle.Read, _.Web.Mobile) { ctx ?=> me ?=>
    replayOf(days, themeKey).map:
      _.fold(notFoundJson("No puzzles to replay")):
        case ((_, replay), angle) =>
          import lila.puzzle.JsonView.given
          JsonOk(Json.obj("replay" -> replay, "angle" -> angle))
  }

  private def replayOf(days: Days, themeKey: String)(using Me) =
    val theme = PuzzleTheme.findOrMix(themeKey)
    val checkedDayOpt = lila.puzzle.PuzzleDashboard.getClosestDay(days)
    env.puzzle.replay(checkedDayOpt, theme.key).map2(_ -> PuzzleAngle(theme))

  def history(page: Int, u: Option[UserStr]) = DashboardPage(u) { _ ?=> user =>
    Reasonable(page):
      WithPuzzlePerf: perf ?=>
        Ok.async:
          env.puzzle
            .history(user.withPerf(perf), page)
            .map:
              views.puzzle.ui.history(user, _)
  }

  def apiBatchSelect(angleStr: String) = AnonOrScoped(_.Puzzle.Read, _.Web.Mobile): ctx ?=>
    WithPuzzlePerf:
      batchSelect(PuzzleAngle.findOrMix(angleStr), reqDifficulty, getInt("nb") | 15).dmap(Ok.apply)

  private def reqDifficulty(using req: RequestHeader) = PuzzleDifficulty.orDefault(~get("difficulty"))
  private def batchSelect(angle: PuzzleAngle, difficulty: PuzzleDifficulty, nb: Int)(using Option[Me], Perf) =
    env.puzzle.batch.nextForMe(angle, difficulty, nb.atMost(50)).flatMap(env.puzzle.jsonView.batch)

  def apiBatchSolve(angleStr: String) = AnonOrScopedBody(parse.json)(_.Puzzle.Write, _.Web.Mobile): ctx ?=>
    ctx.body.body
      .validate[PuzzleForm.batch.SolveData]
      .fold(
        err => BadRequest(err.toString),
        data =>
          val angle = PuzzleAngle.findOrMix(angleStr)
          for
            rounds <- ctx.me match
              case Some(me) =>
                given Me = me
                WithPuzzlePerf:
                  env.puzzle.finisher.batch(angle, data.solutions).map {
                    _.map { (round, rDiff) => env.puzzle.jsonView.roundJson.api(round, rDiff) }
                  }
              case None =>
                data.solutions
                  .sequentiallyVoid { sol => env.puzzle.finisher.incPuzzlePlays(sol.id) }
                  .inject(Nil)
            given Option[Me] <- ctx.me.so(env.user.repo.me)
            nextPuzzles <- WithPuzzlePerf:
              batchSelect(angle, reqDifficulty, ~getInt("nb"))
            result = nextPuzzles ++ Json.obj("rounds" -> rounds)
          yield Ok(result)
      )

  def mobileBcLoad(nid: Long) = Open:
    negotiateJson:
      FoundOk(Puz.numericalId(nid).so(env.puzzle.api.puzzle.find)): puz =>
        WithPuzzlePerf:
          env.puzzle.jsonView.bc(puz)

  // XHR load next play puzzle
  def mobileBcNew = Open:
    NoBot:
      negotiateApi(
        html = notFound,
        api = v =>
          val angle = PuzzleAngle.mix
          WithPuzzlePerf:
            Found(selector.nextPuzzleFor(angle, none, PuzzleDifficulty.fromReqSession(req))): p =>
              JsonOk(jsonView.analysis(p, angle, apiVersion = v.some))
      )

  /* Mobile API: select a bunch of puzzles for offline use */
  def mobileBcBatchSelect = Auth { ctx ?=> _ ?=>
    negotiateJson:
      val nb = getInt("nb").getOrElse(15).atLeast(1).atMost(30)
      WithPuzzlePerf:
        env.puzzle.batch
          .nextForMe(PuzzleDifficulty.default, nb)
          .flatMap: puzzles =>
            env.puzzle.jsonView.bc.batch(puzzles)
          .dmap(Ok(_))
  }

  /* Mobile API: tell the server about puzzles solved while offline */
  def mobileBcBatchSolve = AuthBody(parse.json) { ctx ?=> me ?=>
    negotiateJson:
      import PuzzleForm.bc.*
      ctx.body.body
        .validate[SolveDataBc]
        .fold(
          err => BadRequest(err.toString),
          data =>
            WithPuzzlePerf: perf ?=>
              data.solutions.lastOption
                .flatMap: solution =>
                  Puz.numericalId(solution.id).map(_ -> solution.win)
                .so: (id, solution) =>
                  env.puzzle.finisher(id, PuzzleAngle.mix, solution, chess.Rated.Yes)
                .map:
                  case None =>
                    Ok(env.puzzle.jsonView.bc.userJson(perf.intRating))
                  case Some(round, newPerf) =>
                    env.puzzle.session.onComplete(round, PuzzleAngle.mix)
                    Ok(env.puzzle.jsonView.bc.userJson(newPerf.intRating))
        )
  }

  def mobileBcVote(nid: Long) = AuthBody { ctx ?=> me ?=>
    negotiateJson:
      bindForm(env.puzzle.forms.bc.vote)(
        doubleJsonFormError,
        intVote =>
          Puz.numericalId(nid).so {
            env.puzzle.api.vote.update(_, me, intVote == 1).inject(jsonOkResult)
          }
      )
  }

  def help = Open:
    Ok.snip(lila.web.ui.help.puzzle)

  private def DashboardPage(username: Option[UserStr])(f: Context ?=> lila.user.User => Fu[Result]) =
    Auth { ctx ?=> me ?=>
      meOrFetch(username)
        .flatMapz: user =>
          (fuccess(user.is(me) || isGranted(_.CheatHunter)) >>|
            user.enabled.yes.so(env.clas.api.clas.isTeacherOf(me, user.id))).map:
            _.option(user)
        .flatMap:
          case Some(user) => f(user)
          case None => Redirect(routes.Puzzle.dashboard(Days(30), "home", none))
    }

  private def WithPuzzlePerf[A](f: Perf ?=> Fu[A])(using Option[Me]): Fu[A] =
    WithMyPerf(PerfType.Puzzle)(f)
