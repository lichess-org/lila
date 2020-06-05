package controllers

import play.api.libs.json._
import play.api.mvc._

import lidraughts.api.Context
import lidraughts.app._
import lidraughts.common.{ HTTPRequest, IpAddress, MaxPerSecond }
import lidraughts.game.PdnDump
import lidraughts.pref.Pref.puzzleVariants
import lidraughts.puzzle.{ PuzzleId, Result, Puzzle => PuzzleModel, UserInfos }
import lidraughts.user.UserRepo
import draughts.variant.{ Variant, Standard }
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

  private def renderVariant(variant: Variant, cookie: Option[Cookie])(implicit ctx: Context) =
    env.selector(ctx.me, variant) flatMap { puzzle =>
      renderShow(puzzle, if (ctx.isAuth) "play" else "try") map { h => cookie.fold(Ok(h))(c => Ok(h).withCookies(c)) }
    }

  private def renderShow(puzzle: PuzzleModel, mode: String)(implicit ctx: Context) =
    env.userInfos(ctx.me, puzzle.variant) flatMap { infos =>
      renderJson(puzzle = puzzle, userInfos = infos, mode = mode, voted = none) map { json =>
        views.html.puzzle.show(puzzle, data = json, pref = env.jsonView.pref(ctx.pref))
      }
    }

  def daily = Open { implicit ctx =>
    NoBot {
      OptionFuResult(env.daily.get flatMap {
        _.map(_.id) ?? (id => env.api.puzzle.find(id, Standard))
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
      renderVariant(ctx.pref.puzzleVariant, none)
    }
  }

  private def homeVariant(variant: Variant) = Open { implicit ctx =>
    if (ctx.pref.puzzleVariant != variant)
      controllers.Pref.save("puzzleVariant")(variant.key, ctx) flatMap {
        cookie => renderVariant(variant, cookie.some)
      }
    else renderVariant(variant, none)
  }

  def showOrVariant(something: String) = Variant(something) match {
    case Some(variant) if puzzleVariants.contains(variant) => homeVariant(variant)
    case _ => parseIntOption(something) match {
      case Some(id) => show(id, Standard)
      case _ => Open { implicit ctx => notFound(ctx) }
    }
  }

  def showVariant(id: PuzzleId, key: String) = Variant(key) match {
    case Some(variant) if puzzleVariants.contains(variant) => show(id, variant)
    case _ => Open { implicit ctx => notFound(ctx) }
  }

  private def show(id: PuzzleId, variant: Variant) = Open { implicit ctx =>
    NoBot {
      OptionFuOk(env.api.puzzle.find(id, variant)) { puzzle =>
        renderShow(puzzle, "play")
      }
    }
  }

  def loadStandard(id: PuzzleId) = load(id, Standard)

  def loadVariant(id: PuzzleId, key: String) = Variant(key) match {
    case Some(variant) if puzzleVariants.contains(variant) => load(id, variant)
    case _ => Open { implicit ctx => notFound(ctx) }
  }

  private def load(id: PuzzleId, variant: Variant) = Open { implicit ctx =>
    NoBot {
      XhrOnly {
        OptionFuOk(env.api.puzzle.find(id, variant))(puzzleJson) map (_ as JSON)
      }
    }
  }

  private def puzzleJson(puzzle: PuzzleModel)(implicit ctx: Context) =
    env.userInfos(ctx.me, puzzle.variant) flatMap { infos =>
      renderJson(puzzle, infos, if (ctx.isAuth) "play" else "try", voted = none)
    }

  def newPuzzleStandard = newPuzzle(Standard)

  def newPuzzleVariant(key: String) = Variant(key) match {
    case Some(variant) if puzzleVariants.contains(variant) => newPuzzle(variant)
    case _ => Open { implicit ctx => notFound(ctx) }
  }

  // XHR load next play puzzle
  private def newPuzzle(variant: Variant) = Open { implicit ctx =>
    NoBot {
      XhrOnly {
        env.selector(ctx.me, variant) flatMap puzzleJson map { json =>
          Ok(json) as JSON
        }
      }
    }
  }

  def round2Standard(id: PuzzleId) = round2(id, Standard)

  def round2Variant(id: PuzzleId, key: String) = Variant(key) match {
    case Some(variant) if puzzleVariants.contains(variant) => round2(id, variant)
    case _ => OpenBody { implicit ctx => fuccess(BadRequest("bad variant")) map (_ as JSON) }
  }

  // new API
  private def round2(id: PuzzleId, variant: Variant) = OpenBody { implicit ctx =>
    NoBot {
      implicit val req = ctx.body
      OptionFuResult(env.api.puzzle.find(id, variant)) { puzzle =>
        if (puzzle.mate) lidraughts.mon.puzzle.round.mate()
        else lidraughts.mon.puzzle.round.material()
        env.forms.round.bindFromRequest.fold(
          jsonFormError,
          resultInt => ctx.me match {
            case Some(me) => for {
              (round, mode) <- env.finisher(puzzle, me, Result(resultInt == 1))
              me2 <- if (mode.rated) UserRepo byId me.id map (_ | me) else fuccess(me)
              infos <- env.userInfos(me2, variant)
              voted <- ctx.me.?? {
                env.api.vote.value(puzzle.id, variant, _)
              }
            } yield {
              lidraughts.mon.puzzle.round.user()
              Ok(Json.obj(
                "user" -> lidraughts.puzzle.JsonView.infos(variant)(infos),
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
  }

  def voteStandard(id: PuzzleId) = vote(id, Standard)

  def voteVariant(id: PuzzleId, key: String) = Variant(key) match {
    case Some(variant) if puzzleVariants.contains(variant) => vote(id, variant)
    case _ => AuthBody { implicit ctx => _ => fuccess(BadRequest("bad variant")) map (_ as JSON) }
  }

  private def vote(id: PuzzleId, variant: Variant) = AuthBody { implicit ctx => me =>
    NoBot {
      implicit val req = ctx.body
      env.forms.vote.bindFromRequest.fold(
        jsonFormError,
        vote => env.api.vote.find(id, variant, me) flatMap {
          v => env.api.vote.update(id, variant, me, v, vote == 1)
        } map {
          case (p, a) =>
            if (vote == 1) lidraughts.mon.puzzle.vote.up()
            else lidraughts.mon.puzzle.vote.down()
            Ok(Json.arr(a.value, p.vote.sum))
        }
      ) map (_ as JSON)
    }
  }

  def recentGame = Action.async { req =>
    if (!get("token", req).contains(Env.api.apiToken)) BadRequest.fuccess
    else {
      import akka.pattern.ask
      import makeTimeout.short
      Env.game.recentGoodGameActor ? true mapTo manifest[Option[String]] flatMap {
        _ ?? lidraughts.game.GameRepo.gameWithInitialFen flatMap {
          case Some((game, initialFen)) =>
            Env.api.pdnDump(game, initialFen, none, PdnDump.WithFlags(clocks = false)) map { pdn =>
              Ok(pdn.render)
            }
          case _ =>
            lidraughts.log("puzzle import").info("No recent good game, serving a random one :-/")
            lidraughts.game.GameRepo.findRandomFinished(1000) flatMap {
              _ ?? { game =>
                lidraughts.game.GameRepo.initialFen(game) flatMap { fen =>
                  Env.api.pdnDump(game, fen, none, PdnDump.WithFlags(clocks = false)) map { pdn =>
                    Ok(pdn.render)
                  }
                }
              }
            }
        }
      }
    }
  }

  def importOne = SecureBody(BodyParsers.parse.json)(lidraughts.security.Permission.CreatePuzzles) { implicit ctx => me =>
    Variant(~get("variant", ctx.req)) match {
      case Some(variant) if puzzleVariants.contains(variant) =>
        env.api.puzzle.importOne(ctx.body.body, variant) map { id =>
          val url = if (variant.exotic) s"https://lidraughts.org/training/${variant.key}/$id" else s"https://lidraughts.org/training/$id"
          lidraughts.log("puzzle import").info(s"${me.username} ${ctx.req.remoteAddress} $url")
          Ok(s"ok $url")
        } recover {
          case e =>
            lidraughts.log("puzzle import").warn(s"${ctx.req.remoteAddress} ${e.getMessage}", e)
            BadRequest(e.getMessage)
        }
      case _ => fuccess(BadRequest("bad variant"))
    }
  }

  /* Mobile API: select a bunch of puzzles for offline use */
  def batchSelect(key: String) = Auth { implicit ctx => me =>
    negotiate(
      html = notFound,
      api = _ => Variant(key) match {
        case Some(variant) if puzzleVariants.contains(variant) =>
          for {
            puzzles <- env.batch.select(
              me, variant,
              nb = getInt("nb") getOrElse 50 atLeast 1 atMost 100,
              after = getInt("after")
            )
            userInfo <- env.userInfos(me, variant)
            json <- env.jsonView.batch(puzzles, userInfo, variant)
          } yield Ok(json) as JSON
        case _ => notFoundJson("Invalid variant")
      }
    )
  }

  /* Mobile API: tell the server about puzzles solved while offline */
  def batchSolve(key: String) = AuthBody(BodyParsers.parse.json) { implicit ctx => me =>
    import lidraughts.puzzle.PuzzleBatch._
    ctx.body.body.validate[SolveData].fold(
      err => BadRequest(err.toString).fuccess,
      data => negotiate(
        html = notFound,
        api = _ => Variant(key) match {
          case Some(variant) if puzzleVariants.contains(variant) =>
            for {
              _ <- env.batch.solve(me, variant, data)
              me2 <- UserRepo byId me.id map (_ | me)
              infos <- env.userInfos(me2, variant)
            } yield Ok(Json.obj(
              "user" -> lidraughts.puzzle.JsonView.infos(variant)(infos)
            ))
          case _ => notFoundJson("Invalid variant")
        }
      )
    )
  }

  /* For BC */
  def embed = Action { req =>
    Ok {
      val bg = get("bg", req) | "light"
      val theme = get("theme", req) | "brown"
      val url = s"""${req.domain + routes.Puzzle.frame}?bg=$bg&theme=$theme"""
      s"""document.write("<iframe src='https://$url&embed=" + document.domain + "' class='lidraughts-training-iframe' allowtransparency='true' frameborder='0' style='width: 224px; height: 264px;' title='Lidraughts free online draughts'></iframe>");"""
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
        val config = lidraughts.puzzle.PuzzleActivity.Config(
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
