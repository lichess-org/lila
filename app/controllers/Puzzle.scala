package controllers

import play.api.libs.json._
import play.api.mvc._

import lidraughts.api.Context
import lidraughts.app._
import lidraughts.game.PdnDump
import lidraughts.puzzle.{ PuzzleId, Result, Puzzle => PuzzleModel, UserInfos }
import lidraughts.user.UserRepo
import views._

object Puzzle extends LidraughtsController {

  private def env = Env.puzzle

  private def renderJson(
    puzzle: PuzzleModel,
    userInfos: Option[UserInfos],
    mode: String,
    voted: Option[Boolean],
    round: Option[lidraughts.puzzle.Round] = None,
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
    OptionFuResult(env.daily.get flatMap {
      _.map(_.id) ?? env.api.puzzle.find
    }) { puzzle =>
      negotiate(
        html = renderShow(puzzle, "play") map { Ok(_) },
        api = _ => puzzleJson(puzzle) map { Ok(_) }
      ) map { NoCache(_) }
    }
  }

  def home = Open { implicit ctx =>
    env.selector(ctx.me) flatMap { puzzle =>
      renderShow(puzzle, ctx.isAuth.fold("play", "try")) map { Ok(_) }
    }
  }

  def show(id: PuzzleId) = Open { implicit ctx =>
    OptionFuOk(env.api.puzzle find id) { puzzle =>
      renderShow(puzzle, "play")
    }
  }

  def load(id: PuzzleId) = Open { implicit ctx =>
    XhrOnly {
      OptionFuOk(env.api.puzzle find id)(puzzleJson) map (_ as JSON)
    }
  }

  private def puzzleJson(puzzle: PuzzleModel)(implicit ctx: Context) =
    env userInfos ctx.me flatMap { infos =>
      renderJson(puzzle, infos, ctx.isAuth.fold("play", "try"), voted = none)
    }

  // XHR load next play puzzle
  def newPuzzle = Open { implicit ctx =>
    XhrOnly {
      env.selector(ctx.me) flatMap puzzleJson map { json =>
        Ok(json) as JSON
      }
    }
  }

  // new API
  def round2(id: PuzzleId) = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    OptionFuResult(env.api.puzzle find id) { puzzle =>
      if (puzzle.mate) lidraughts.mon.puzzle.round.mate()
      else lidraughts.mon.puzzle.round.material()
      env.forms.round.bindFromRequest.fold(
        err => fuccess(BadRequest(errorsAsJson(err))),
        resultInt => ctx.me match {
          case Some(me) => for {
            (round, mode) <- env.finisher(puzzle, me, Result(resultInt == 1))
            me2 <- mode.rated.fold(UserRepo byId me.id map (_ | me), fuccess(me))
            infos <- env userInfos me2
            voted <- ctx.me.?? { env.api.vote.value(puzzle.id, _) }
          } yield {
            lidraughts.mon.puzzle.round.user()
            Ok(Json.obj(
              "user" -> lidraughts.puzzle.JsonView.infos(false)(infos),
              "round" -> lidraughts.puzzle.JsonView.round(round),
              "voted" -> voted
            ))
          }
          case None =>
            lidraughts.mon.puzzle.round.anon()
            env.finisher.incPuzzleAttempts(puzzle)
            Ok(Json.obj("user" -> false)).fuccess
        }
      ) map (_ as JSON)
    }
  }

  def vote(id: PuzzleId) = AuthBody { implicit ctx => me =>
    implicit val req = ctx.body
    env.forms.vote.bindFromRequest.fold(
      err => fuccess(BadRequest(errorsAsJson(err))),
      vote => env.api.vote.find(id, me) flatMap {
        v => env.api.vote.update(id, me, v, vote == 1)
      } map {
        case (p, a) =>
          if (vote == 1) lidraughts.mon.puzzle.vote.up()
          else lidraughts.mon.puzzle.vote.down()
          Ok(Json.arr(a.value, p.vote.sum))
      }
    ) map (_ as JSON)
  }

  def recentGame = Action.async { req =>
    if (!get("token", req).contains(Env.api.apiToken)) BadRequest.fuccess
    else {
      import akka.pattern.ask
      import makeTimeout.short
      Env.game.recentGoodGameActor ? true mapTo manifest[Option[String]] flatMap {
        _ ?? lidraughts.game.GameRepo.gameWithInitialFen flatMap {
          case Some((game, initialFen)) =>
            Env.api.pdnDump(game, initialFen.map(_.value), PdnDump.WithFlags(clocks = false)) map { pdn =>
              Ok(pdn.render)
            }
          case _ =>
            lidraughts.log("puzzle import").info("No recent good game, serving a random one :-/")
            lidraughts.game.GameRepo.findRandomFinished(1000) flatMap {
              _ ?? { game =>
                lidraughts.game.GameRepo.initialFen(game) flatMap { fen =>
                  Env.api.pdnDump(game, fen, PdnDump.WithFlags(clocks = false)) map { pdn =>
                    Ok(pdn.render)
                  }
                }
              }
            }
        }
      }
    }
  }

  def importOne = Action.async(parse.json) { implicit req =>
    env.api.puzzle.importOne(req.body, ~get("token", req)) map { id =>
      val url = s"https://lidraughts.org/training/$id"
      lidraughts.log("puzzle import").info(s"${req.remoteAddress} $url")
      Ok(s"kthxbye $url")
    } recover {
      case e =>
        lidraughts.log("puzzle import").warn(s"${req.remoteAddress} ${e.getMessage}", e)
        BadRequest(e.getMessage)
    }
  }

  /* Mobile API: select a bunch of puzzles for offline use */
  def batchSelect = Auth { implicit ctx => me =>
    negotiate(
      html = notFound,
      api = _ => for {
        puzzles <- env.batch.select(me, getInt("nb") getOrElse 50 atLeast 1 atMost 100)
        userInfo <- env userInfos me
        json <- env.jsonView.batch(puzzles, userInfo)
      } yield Ok(json) as JSON
    )
  }

  /* Mobile API: tell the server about puzzles solved while offline */
  def batchSolve = AuthBody(BodyParsers.parse.json) { implicit ctx => me =>
    import lidraughts.puzzle.PuzzleBatch._
    ctx.body.body.validate[SolveData].fold(
      err => BadRequest(err.toString).fuccess,
      data => negotiate(
        html = notFound,
        api = _ => env.batch.solve(me, data)
      )
    )
  }

  def embed = Action { req =>
    Ok {
      val bg = get("bg", req) | "light"
      val theme = get("theme", req) | "brown"
      val url = s"""${req.domain + routes.Puzzle.frame}?bg=$bg&theme=$theme"""
      s"""document.write("<iframe src='//$url&embed=" + document.domain + "' class='lidraughts-training-iframe' allowtransparency='true' frameBorder='0' style='width: 224px; height: 264px;' title='Lidraughts free online draughts'></iframe>");"""
    } as JAVASCRIPT withHeaders (CACHE_CONTROL -> "max-age=86400")
  }

  def frame = Open { implicit ctx =>
    OptionOk(env.daily.get) { daily =>
      html.puzzle.embed(daily)
    }
  }
}
