package controllers

import cats.mtl.Handle.*
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
import lila.common.HTTPRequest
import lila.common.Json.given

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
    settings <- ctx.user.traverse(env.puzzle.session.getSettings)
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
  )(id: PuzzleId, angle: PuzzleAngle, mobileBc: Boolean)(using BodyContext[A]): Fu[Result] =
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
      puzzles <- user.traverse(env.puzzle.api.puzzle.of(_, page))
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
        vote =>
          for _ <- env.puzzle.api.vote.update(id, me, vote)
          yield jsonOkResult
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

  def apiBatchVoteThemes = SecuredScopedBody(_.PuzzleCurator)(_.Puzzle.Write) { _ ?=> me ?=>
    bindForm(env.puzzle.forms.batchVotes)(
      jsonFormError,
      _.votes
        .sequentially(puzzleVotes =>
          puzzleVotes.themes
            .sequentially: themeVote =>
              allow:
                env.puzzle.api.theme
                  .vote(puzzleVotes.puzzleId, themeVote.theme, themeVote.vote)
                  .inject(none)
              .rescue: err =>
                fuccess(Json.obj("theme" -> themeVote.theme, "msg" -> err.message).some)
            .map:
              _.flatten.map: errors =>
                Json.obj("puzzleId" -> puzzleVotes.puzzleId, "errors" -> errors)
        )
        .map(_.flatten)
        .map:
          case Nil => jsonOkResult
          case errors => BadRequest(jsonError(errors))
    )
  }

  def voteTheme(id: PuzzleId, themeStr: String) = AuthOrScopedBody(_.Puzzle.Write) { _ ?=> me ?=>
    NoBot:
      import lila.puzzle.PuzzleTheme.VoteError.*
      bindForm(env.puzzle.forms.themeVote)(
        doubleJsonFormError,
        vote =>
          allow:
            env.puzzle.api.theme.vote(id, themeStr, vote).inject(jsonOkResult)
          .rescue:
            case Fail(msg) => BadRequest(jsonError(msg))
            case Unchanged => jsonOkResult
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
          Puz.toId(angleOrId) match
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

  private val fetchRateLimit =
    env.security.ipTrust.rateLimit(300, 1.hour, "puzzle.fetch.ip", _.antiScraping(dch = 5, others = 1))

  def apiNext = AnonOrScoped(_.Puzzle.Read):
    fetchRateLimit(rateLimited, cost = if ctx.isAuth then 1 else 5):
      WithPuzzlePerf:
        val angle = PuzzleAngle.findOrMix(~get("angle"))
        val settings = reqSettings
        FoundOk(selector.nextPuzzleFor(angle, settings.color.map(some), settings.difficulty.some)):
          env.puzzle.jsonView(_, none, none)

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
    val nb = getInt("nb") | 15
    val cost =
      if ctx.isMobileOauth then 0
      else if HTTPRequest.isLichessMobile(ctx.req) then nb / 5
      else if ctx.isAuth then nb / 3
      else nb
    fetchRateLimit(rateLimited, cost = cost):
      WithPuzzlePerf:
        for puzzles <- batchSelect(PuzzleAngle.findOrMix(angleStr), reqSettings, nb)
        yield Ok(puzzles)

  private def reqSettings(using req: RequestHeader) = PuzzleSettings(
    PuzzleDifficulty.orDefault(~get("difficulty")),
    get("color").flatMap(Color.fromName)
  )

  private def batchSelect(angle: PuzzleAngle, settings: PuzzleSettings, nb: Int)(using Option[Me], Perf) =
    env.puzzle.batch.nextForMe(angle, settings, nb.atMost(50)).flatMap(env.puzzle.jsonView.batch)

  private val solveRateLimit =
    env.security.ipTrust.rateLimit(400, 1.hour, "puzzle.solve.ip", _.proxyMultiplier(2))

  def apiBatchSolve(angleStr: String) = AnonOrScopedBody(parse.json)(_.Puzzle.Write, _.Web.Mobile): ctx ?=>
    ctx.body.body
      .validate[PuzzleForm.batch.SolveData]
      .fold(
        err => BadRequest(err.toString),
        data =>
          val cost = data.solutions.size * {
            if ctx.isMobileOauth then 1 else if ctx.isAuth then 2 else 5
          }
          solveRateLimit(rateLimited, cost = cost):
            val angle = PuzzleAngle.findOrMix(angleStr)
            for
              rounds <- ctx.me match
                case Some(me) =>
                  given Me = me
                  WithPuzzlePerf:
                    for
                      solves <- env.puzzle.finisher.batch(angle, data.solutions)
                      _ <- env.puzzle.session.onComplete(me.userId, angle, solves.size)
                    yield solves.map(env.puzzle.jsonView.roundJson.api.tupled)
                case None =>
                  data.solutions
                    .sequentiallyVoid { sol => env.puzzle.finisher.incPuzzlePlays(sol.id) }
                    .inject(Nil)
              given Option[Me] <- ctx.me.so(env.user.repo.me)
              nextPuzzles <- WithPuzzlePerf:
                batchSelect(angle, reqSettings, ~getInt("nb"))
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
                    env.puzzle.session.onComplete(round.userId, PuzzleAngle.mix)
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
