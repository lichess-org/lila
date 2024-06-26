package controllers

import play.api.data.Form
import play.api.libs.json._
import scala.util.chaining._
import views._

import lila.api.BodyContext
import lila.api.Context
import lila.app._
import lila.common.config.MaxPerSecond
import lila.puzzle.PuzzleForm.RoundData
import lila.puzzle.PuzzleTheme
import lila.puzzle.{ Puzzle => Puz, PuzzleDashboard, PuzzleDifficulty, PuzzleReplay }

final class Puzzle(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  private def renderJson(
      puzzle: Puz,
      theme: PuzzleTheme,
      replay: Option[PuzzleReplay] = None,
      newUser: Option[lila.user.User] = None
  )(implicit
      ctx: Context
  ): Fu[JsObject] =
    env.puzzle.jsonView(puzzle = puzzle, theme = theme, replay = replay, user = newUser orElse ctx.me)

  private def renderShow(
      puzzle: Puz,
      theme: PuzzleTheme,
      themeView: Boolean = true, // not tied to a single puzzle
      replay: Option[PuzzleReplay] = None
  )(implicit
      ctx: Context
  ) =
    renderJson(puzzle, theme, replay) zip
      ctx.me.??(u => env.puzzle.session.getDifficulty(u) dmap some) map { case (json, difficulty) =>
        Ok(
          views.html.puzzle
            .show(puzzle, theme, json, env.puzzle.jsonView.pref(ctx.pref), themeView, difficulty)
        ).enableSharedArrayBuffer
      }

  def daily =
    Open { implicit ctx =>
      NoBot {
        OptionFuResult(env.puzzle.daily.get) { daily =>
          negotiate(
            html = renderShow(daily.puzzle, PuzzleTheme.mix),
            api = _ => renderJson(daily.puzzle, PuzzleTheme.mix) dmap { Ok(_) }
          ) dmap (_.noCache)
        }
      }
    }

  def apiDaily =
    Action.async { implicit req =>
      env.puzzle.daily.get flatMap {
        _.fold(NotFound.fuccess) { daily =>
          JsonOk(env.puzzle.jsonView(daily.puzzle, PuzzleTheme.mix, none, none)(reqLang))
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
          onComplete(env.puzzle.forms.round)(pid, PuzzleTheme findOrAny themeStr)
        }
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

  private def onComplete[A](form: Form[RoundData])(id: Puz.Id, theme: PuzzleTheme)(implicit
      ctx: BodyContext[A]
  ) = {
    implicit val req = ctx.body
    lila.mon.puzzle.round.attempt(ctx.isAuth, theme.key.value).increment()
    form
      .bindFromRequest()
      .fold(
        jsonFormError,
        data =>
          {
            ctx.me match {
              case Some(me) =>
                env.puzzle.finisher(id, theme.key, me, data.result) flatMap {
                  _ ?? { case (round, perf) =>
                    val newUser = me.copy(perfs = me.perfs.copy(puzzle = perf))
                    for {
                      _ <- env.puzzle.session.onComplete(round, theme.key)
                      json <- data.replayDays match {
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
                            next <- env.puzzle.replay(me, replayDays.some, theme.key)
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
        case None if themeOrId.sizeIs == Puz.idSize =>
          OptionFuResult(env.puzzle.api.puzzle find Puz.Id(themeOrId)) { puz =>
            ctx.me.?? { me =>
              !env.puzzle.api.round.exists(me, puz.id) map {
                _ ?? env.puzzle.api.casual.set(me, puz.id)
              }
            } >>
              renderShow(puz, PuzzleTheme.mix, themeView = false)
          }
        case None => fuccess(Redirect(routes.Puzzle.home))
      }
    }
  }

  def showWithTheme(themeKey: String, id: String) = Open { implicit ctx =>
    NoBot {
      val theme = PuzzleTheme.findOrAny(themeKey)
      OptionFuResult(env.puzzle.api.puzzle find Puz.Id(id)) { puzzle =>
        if (puzzle.themes contains theme.key) renderShow(puzzle, theme, themeView = false)
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
      val theme         = PuzzleTheme.findOrAny(themeKey)
      val checkedDayOpt = PuzzleDashboard.getClosestDay(days)
      env.puzzle.replay(me, checkedDayOpt, theme.key) flatMap {
        case None                   => Redirect(routes.Puzzle.dashboard(days, "home")).fuccess
        case Some((puzzle, replay)) => renderShow(puzzle, theme, themeView = true, replay.some)
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
              Ok(views.html.puzzle.history(user, history))
            }
          }
        }
    }

  def newPuzzlesForm =
    Auth { implicit ctx => _ =>
      Ok(html.puzzle.form(env.puzzle.forms.newPuzzles)).fuccess
    }

  def addPuzzles =
    AuthBody { implicit ctx => me =>
      implicit val body = ctx.body
      env.puzzle.forms.newPuzzles
        .bindFromRequest()
        .fold(
          _ => BadRequest.fuccess,
          { case (sfensStr, source) =>
            val sfens = augmentString(sfensStr).linesIterator.map(shogi.format.forsyth.Sfen.clean).toList
            env.fishnet.api.addPuzzles(sfens.take(5), source, me.id) inject Redirect(
              routes.Puzzle.submitted()
            )
          }
        )
    }

  def submitted(name: Option[String], page: Int) =
    Open { implicit ctx =>
      val fixed = name.map(_.trim).filter(_.nonEmpty)
      fixed.??(env.user.repo.enabledNamed) orElse fuccess(ctx.me) flatMap { userOpt =>
        userOpt ?? { u =>
          (env.puzzle.api.puzzle.submitted(u, page) zip env.fishnet.api.queuedPuzzles(u.id)) dmap some
        } map { case res =>
          Ok(views.html.puzzle.submitted(~fixed, userOpt, res.map(_._1), res.map(_._2)))
        }
      }
    }

  // select a bunch of puzzles for offline use
  // or get a single puzzle I guess
  def mobileShow(themeOrId: String) =
    Open { implicit ctx =>
      negotiate(
        html = notFound,
        api = _ =>
          PuzzleTheme.find(themeOrId) match {
            case None if themeOrId.sizeIs == Puz.idSize =>
              OptionFuResult(env.puzzle.api.puzzle find Puz.Id(themeOrId)) { puz =>
                ctx.me.?? { me =>
                  !env.puzzle.api.round.exists(me, puz.id) map {
                    _ ?? env.puzzle.api.casual.set(me, puz.id)
                  }
                } >> Ok(env.puzzle.jsonView.mobile(Seq(puz), PuzzleTheme.mix, ctx.me)).fuccess
              }
            case themeOpt => batch(themeOpt.getOrElse(PuzzleTheme.mix)) dmap { Ok(_) }
          }
      )
    }

  // tell the server about puzzles solved while offline
  val maxOfflineBatch = 30
  def mobileSolve =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      negotiate(
        html = notFound,
        api = _ => {
          env.puzzle.forms.mobile.solutions
            .bindFromRequest()
            .fold(
              jsonFormError,
              solutions =>
                ctx.me match {
                  case Some(me) =>
                    env.puzzle.finisher.batch(
                      me,
                      solutions.take(maxOfflineBatch)
                    ) map {
                      _.map { case (round, rDiff) => env.puzzle.jsonView.roundJsonApi(round, rDiff) }
                    } dmap { JsonOk(_) }
                  case None =>
                    env.puzzle.finisher.batchIncPuzzlePlays(
                      solutions.take(maxOfflineBatch).flatMap(sol => Puz.toId(sol.id))
                    ) inject { jsonOkResult }
                }
            )
        }
      )
    }

  private def batch(theme: PuzzleTheme)(implicit ctx: Context) =
    env.puzzle.batch.nextFor(ctx.me, theme) map { puzzles =>
      env.puzzle.jsonView.mobile(puzzles, theme, ctx.me)
    }

}
