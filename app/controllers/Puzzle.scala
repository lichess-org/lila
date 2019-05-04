package controllers

import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.{ HTTPRequest, IpAddress, MaxPerSecond }
import lila.game.PgnDump
import lila.puzzle.{ PuzzleId, Result, Puzzle => PuzzleModel, UserInfos }
import lila.user.UserRepo
import views._

object Puzzle extends LilaController {

  private def env = Env.puzzle

  private def renderJson(
    puzzle: PuzzleModel,
    userInfos: Option[UserInfos],
    mode: String,
    voted: Option[Boolean],
    round: Option[lila.puzzle.Round] = None,
    result: Option[Result] = None
  )(implicit ctx: Context): Fu[JsObject] = env.jsonView(
    puzzle = puzzle,
    userInfos = userInfos,
    round = round,
    mode = mode,
    mobileApi = ctx.mobileApiVersion,
    result = result,
    voted = voted
  )

  private def renderShow(puzzle: PuzzleModel, mode: String)(implicit ctx: Context) =
    env userInfos ctx.me flatMap { infos =>
      renderJson(puzzle = puzzle, userInfos = infos, mode = mode, voted = none) map { json =>
        views.html.puzzle.show(puzzle, data = json, pref = env.jsonView.pref(ctx.pref))
      }
    }

  def daily = Open { implicit ctx =>
    NoBot {
      OptionFuResult(env.daily.get flatMap {
        _.map(_.id) ?? env.api.puzzle.find
      }) { puzzle =>
        negotiate(
          html = renderShow(puzzle, "play") map { Ok(_) },
          api = _ => puzzleJson(puzzle) map { Ok(_) }
        ) map { NoCache(_) }
      }
    }
  }

  def home = Open { implicit ctx =>
    NoBot {
      env.selector(ctx.me) flatMap { puzzle =>
        renderShow(puzzle, if (ctx.isAuth) "play" else "try") map { Ok(_) }
      }
    }
  }

  def show(id: PuzzleId) = Open { implicit ctx =>
    NoBot {
      OptionFuOk(env.api.puzzle find id) { puzzle =>
        renderShow(puzzle, "play")
      }
    }
  }

  def load(id: PuzzleId) = Open { implicit ctx =>
    NoBot {
      XhrOnly {
        OptionFuOk(env.api.puzzle find id)(puzzleJson) map (_ as JSON)
      }
    }
  }

  private def puzzleJson(puzzle: PuzzleModel)(implicit ctx: Context) =
    env userInfos ctx.me flatMap { infos =>
      renderJson(puzzle, infos, if (ctx.isAuth) "play" else "try", voted = none)
    }

  // XHR load next play puzzle
  def newPuzzle = Open { implicit ctx =>
    NoBot {
      XhrOnly {
        env.selector(ctx.me) flatMap puzzleJson map { json =>
          Ok(json) as JSON
        }
      }
    }
  }

  // mobile app BC
  def round(id: PuzzleId) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    OptionFuResult(env.api.puzzle find id) { puzzle =>
      if (puzzle.mate) lila.mon.puzzle.round.mate()
      else lila.mon.puzzle.round.material()
      env.forms.round.bindFromRequest.fold(
        jsonFormError,
        resultInt => {
          val result = Result(resultInt == 1)
          ctx.me match {
            case Some(me) => for {
              (round, mode) <- env.finisher(puzzle, me, result, mobile = true)
              me2 <- if (mode.rated) UserRepo byId me.id map (_ | me) else fuccess(me)
              infos <- env userInfos me2
              voted <- ctx.me.?? { env.api.vote.value(puzzle.id, _) }
              data <- renderJson(puzzle, infos.some, "view", voted = voted, result = result.some, round = round.some)
            } yield {
              lila.mon.puzzle.round.user()
              val d2 = if (mode.rated) data else data ++ Json.obj("win" -> result.win)
              Ok(d2)
            }
            case None =>
              lila.mon.puzzle.round.anon()
              env.finisher.incPuzzleAttempts(puzzle)
              renderJson(puzzle, none, "view", result = result.some, voted = none) map { data =>
                val d2 = data ++ Json.obj("win" -> result.win)
                Ok(d2)
              }
          }
        }
      ) map (_ as JSON)
    }
  }

  // new API
  def round2(id: PuzzleId) = OpenBody { implicit ctx =>
    NoBot {
      implicit val req = ctx.body
      OptionFuResult(env.api.puzzle find id) { puzzle =>
        if (puzzle.mate) lila.mon.puzzle.round.mate()
        else lila.mon.puzzle.round.material()
        env.forms.round.bindFromRequest.fold(
          jsonFormError,
          resultInt => ctx.me match {
            case Some(me) => for {
              (round, mode) <- env.finisher(
                puzzle = puzzle,
                user = me,
                result = Result(resultInt == 1),
                mobile = lila.api.Mobile.Api.requested(ctx.req)
              )
              me2 <- if (mode.rated) UserRepo byId me.id map (_ | me) else fuccess(me)
              infos <- env userInfos me2
              voted <- ctx.me.?? { env.api.vote.value(puzzle.id, _) }
            } yield {
              lila.mon.puzzle.round.user()
              Ok(Json.obj(
                "user" -> lila.puzzle.JsonView.infos(false)(infos),
                "round" -> lila.puzzle.JsonView.round(round),
                "voted" -> voted
              ))
            }
            case None =>
              lila.mon.puzzle.round.anon()
              env.finisher.incPuzzleAttempts(puzzle)
              Ok(Json.obj("user" -> false)).fuccess
          }
        ) map (_ as JSON)
      }
    }
  }

  def vote(id: PuzzleId) = AuthBody { implicit ctx => me =>
    NoBot {
      implicit val req = ctx.body
      env.forms.vote.bindFromRequest.fold(
        jsonFormError,
        vote => env.api.vote.find(id, me) flatMap {
          v => env.api.vote.update(id, me, v, vote == 1)
        } map {
          case (p, a) =>
            if (vote == 1) lila.mon.puzzle.vote.up()
            else lila.mon.puzzle.vote.down()
            Ok(Json.arr(a.value, p.vote.sum))
        }
      ) map (_ as JSON)
    }
  }

  /* Mobile API: select a bunch of puzzles for offline use */
  def batchSelect = Auth { implicit ctx => me =>
    negotiate(
      html = notFound,
      api = _ => for {
        puzzles <- env.batch.select(
          me,
          nb = getInt("nb") getOrElse 50 atLeast 1 atMost 100,
          after = getInt("after")
        )
        userInfo <- env userInfos me
        json <- env.jsonView.batch(puzzles, userInfo)
      } yield Ok(json) as JSON
    )
  }

  /* Mobile API: tell the server about puzzles solved while offline */
  def batchSolve = AuthBody(BodyParsers.parse.json) { implicit ctx => me =>
    import lila.puzzle.PuzzleBatch._
    ctx.body.body.validate[SolveData].fold(
      err => BadRequest(err.toString).fuccess,
      data => negotiate(
        html = notFound,
        api = _ => for {
          _ <- env.batch.solve(me, data)
          me2 <- UserRepo byId me.id map (_ | me)
          infos <- env userInfos me2
        } yield Ok(Json.obj(
          "user" -> lila.puzzle.JsonView.infos(false)(infos)
        ))
      )
    )
  }

  /* For BC */
  def embed = Action { req =>
    Ok {
      val bg = get("bg", req) | "light"
      val theme = get("theme", req) | "brown"
      val url = s"""${req.domain + routes.Puzzle.frame}?bg=$bg&theme=$theme"""
      s"""document.write("<iframe src='https://$url&embed=" + document.domain + "' class='lichess-training-iframe' allowtransparency='true' frameborder='0' style='width: 224px; height: 264px;' title='Lichess free online chess'></iframe>");"""
    } as JAVASCRIPT withHeaders (CACHE_CONTROL -> "max-age=86400")
  }

  def frame = Action.async { implicit req =>
    env.daily.get map {
      case None => NotFound
      case Some(daily) => html.puzzle.embed(daily)
    }
  }

  def activity = Scoped(_.Puzzle.Read) { req => me =>
    Api.GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
      Api.GlobalLinearLimitPerUserOption(me.some) {
        val config = lila.puzzle.PuzzleActivity.Config(
          user = me,
          max = getInt("max", req) map (_ atLeast 1),
          perSecond = MaxPerSecond(20)
        )
        Ok.chunked(env.activity.stream(config)).withHeaders(
          noProxyBufferHeader,
          CONTENT_TYPE -> ndJsonContentType
        ).fuccess
      }
    }
  }

}
