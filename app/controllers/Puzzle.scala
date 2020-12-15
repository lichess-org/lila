package controllers

import play.api.libs.json._
import scala.util.chaining._
import views._

import lila.api.Context
import lila.app._
import lila.common.config.MaxPerSecond
import lila.puzzle.{ PuzzleId, Result, Puzzle => PuzzleModel, UserInfos }

final class Puzzle(
    env: Env,
    apiC: => Api
) extends LilaController(env) {

  private def renderJson(
      puzzle: PuzzleModel,
      userInfos: Option[UserInfos],
      mode: String,
      voted: Option[Boolean],
      round: Option[lila.puzzle.Round] = None
  )(implicit ctx: Context): Fu[JsObject] =
    env.puzzle.jsonView(
      puzzle = puzzle,
      userInfos = userInfos,
      round = round,
      mode = mode,
      mobileApi = ctx.mobileApiVersion,
      voted = voted
    )

  private def renderShow(puzzle: PuzzleModel, mode: String)(implicit ctx: Context) =
    env.puzzle userInfos ctx.me flatMap { infos =>
      renderJson(puzzle = puzzle, userInfos = infos, mode = mode, voted = none) map { json =>
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
            html = renderShow(puzzle, "play"),
            api = _ => puzzleJson(puzzle) map { Ok(_) }
          ) map NoCache
        }
      }
    }

  def home =
    Open { implicit ctx =>
      NoBot {
        env.puzzle.selector(ctx.me) flatMap { puzzle =>
          renderShow(puzzle, if (ctx.isAuth) "play" else "try")
        }
      }
    }

  def show(id: PuzzleId) =
    Open { implicit ctx =>
      if (id < env.puzzle.idMin) notFound
      else
        NoBot {
          OptionFuResult(env.puzzle.api.puzzle find id) { puzzle =>
            renderShow(puzzle, "play")
          }
        }
    }

  def load(id: PuzzleId) =
    Open { implicit ctx =>
      NoBot {
        XhrOnly {
          OptionFuOk(env.puzzle.api.puzzle find id)(puzzleJson) map (_ as JSON)
        }
      }
    }

  private def puzzleJson(puzzle: PuzzleModel)(implicit ctx: Context) =
    env.puzzle userInfos ctx.me flatMap { infos =>
      renderJson(puzzle, infos, if (ctx.isAuth) "play" else "try", voted = none)
    }

  // XHR load next play puzzle
  def newPuzzle =
    Open { implicit ctx =>
      NoBot {
        XhrOnly {
          env.puzzle.selector(ctx.me) flatMap puzzleJson map { json =>
            Ok(json) as JSON
          }
        }
      }
    }

  // mobile app BC
  def round(id: PuzzleId) =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      OptionFuResult(env.puzzle.api.puzzle find id) { puzzle =>
        lila.mon.puzzle.round.attempt(puzzle.mate, ctx.isAuth, "old").increment()
        env.puzzle.forms.round
          .bindFromRequest()
          .fold(
            jsonFormError,
            resultInt => {
              val result = Result(resultInt == 1)
              ctx.me match {
                case Some(me) =>
                  for {
                    isStudent <- env.clas.api.student.isStudent(me.id)
                    (round, mode) <-
                      env.puzzle.finisher(puzzle, me, result, mobile = true, isStudent = isStudent)
                    me2   <- if (mode.rated) env.user.repo byId me.id map (_ | me) else fuccess(me)
                    infos <- env.puzzle userInfos me2
                    voted <- ctx.me.?? { env.puzzle.api.vote.value(puzzle.id, _) }
                    data  <- renderJson(puzzle, infos.some, "view", voted = voted, round = round.some)
                  } yield {
                    val d2 = if (mode.rated) data else data ++ Json.obj("win" -> result.win)
                    Ok(d2)
                  }
                case None =>
                  env.puzzle.finisher.incPuzzleAttempts(puzzle)
                  renderJson(puzzle, none, "view", voted = none) map { data =>
                    val d2 = data ++ Json.obj("win" -> result.win)
                    Ok(d2)
                  }
              }
            }
          ) map (_ as JSON)
      }
    }

  // new API
  def round2(id: PuzzleId) =
    OpenBody { implicit ctx =>
      NoBot {
        implicit val req = ctx.body
        OptionFuResult(env.puzzle.api.puzzle find id) { puzzle =>
          lila.mon.puzzle.round.attempt(puzzle.mate, ctx.isAuth, "new").increment()
          env.puzzle.forms.round
            .bindFromRequest()
            .fold(
              jsonFormError,
              resultInt =>
                ctx.me match {
                  case Some(me) =>
                    for {
                      isStudent <- env.clas.api.student.isStudent(me.id)
                      (round, mode) <- env.puzzle.finisher(
                        puzzle = puzzle,
                        user = me,
                        result = Result(resultInt == 1),
                        mobile = lila.api.Mobile.Api.requested(ctx.req),
                        isStudent = isStudent
                      )
                      me2   <- if (mode.rated) env.user.repo byId me.id map (_ | me) else fuccess(me)
                      infos <- env.puzzle userInfos me2
                      voted <- ctx.me.?? { env.puzzle.api.vote.value(puzzle.id, _) }
                    } yield Ok(
                      Json.obj(
                        "user"  -> lila.puzzle.JsonView.infos(isOldMobile = false)(infos),
                        "round" -> lila.puzzle.JsonView.round(round),
                        "voted" -> voted
                      )
                    )
                  case None =>
                    env.puzzle.finisher.incPuzzleAttempts(puzzle)
                    Ok(Json.obj("user" -> false)).fuccess
                }
            ) map (_ as JSON)
        }
      }
    }

  def vote(id: PuzzleId) =
    AuthBody { implicit ctx => me =>
      NoBot {
        implicit val req = ctx.body
        env.puzzle.forms.vote
          .bindFromRequest()
          .fold(
            jsonFormError,
            vote =>
              env.puzzle.api.vote.find(id, me) flatMap { v =>
                env.puzzle.api.vote.update(id, me, v, vote == 1)
              } map { case (p, a) =>
                if (vote == 1) lila.mon.puzzle.vote.up.increment()
                else lila.mon.puzzle.vote.down.increment()
                Ok(Json.arr(a.value, p.vote.sum))
              }
          ) map (_ as JSON)
      }
    }

  /* Mobile API: select a bunch of puzzles for offline use */
  def batchSelect =
    Auth { implicit ctx => me =>
      negotiate(
        html = notFound,
        api = _ =>
          for {
            puzzles <- env.puzzle.batch.select(
              me,
              nb = getInt("nb") getOrElse 50 atLeast 1 atMost 100,
              after = getInt("after")
            )
            userInfo <- env.puzzle userInfos me
            json     <- env.puzzle.jsonView.batch(puzzles, userInfo)
          } yield Ok(json) as JSON
      )
    }

  /* Mobile API: tell the server about puzzles solved while offline */
  def batchSolve =
    AuthBody(parse.json) { implicit ctx => me =>
      import lila.puzzle.PuzzleBatch._
      ctx.body.body
        .validate[SolveData]
        .fold(
          err => BadRequest(err.toString).fuccess,
          data =>
            negotiate(
              html = notFound,
              api = _ =>
                for {
                  _     <- env.puzzle.batch.solve(me, data)
                  me2   <- env.user.repo byId me.id map (_ | me)
                  infos <- env.puzzle userInfos me2
                } yield Ok(
                  Json.obj(
                    "user" -> lila.puzzle.JsonView.infos(isOldMobile = false)(infos)
                  )
                )
            )
        )
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
