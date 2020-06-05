package controllers

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.mvc._
import scala.concurrent.duration._

import lidraughts.api.GameApiV2
import lidraughts.app._
import lidraughts.common.{ MaxPerSecond, HTTPRequest }
import lidraughts.game.{ GameRepo, Game => GameModel }
import lidraughts.pref.Pref.{ default => defaultPref }
import views._

object Game extends LidraughtsController {

  def delete(gameId: String) = Auth { implicit ctx => me =>
    OptionFuResult(GameRepo game gameId) { game =>
      if (game.pdnImport.flatMap(_.user) ?? (me.id==)) {
        Env.hub.bookmark ! lidraughts.hub.actorApi.bookmark.Remove(game.id)
        (GameRepo remove game.id) >>
          (lidraughts.analyse.AnalysisRepo remove game.id) >>
          Env.game.cached.clearNbImportedByCache(me.id) inject
          Redirect(routes.User.show(me.username))
      } else fuccess {
        Redirect(routes.Round.watcher(game.id, game.firstColor.name))
      }
    }
  }

  def exportOne(id: String) = Open { implicit ctx =>
    OptionFuResult(GameRepo game id) { game =>
      if (game.playable) BadRequest("Can't export PDN of game in progress").fuccess
      else {
        val config = GameApiV2.OneConfig(
          format = if (HTTPRequest acceptsJson ctx.req) GameApiV2.Format.JSON else GameApiV2.Format.PDN,
          imported = getBool("imported"),
          flags = requestPdnFlags(ctx.req, ctx.pref.draughtsResult, extended = true, ctx.pref.canAlgebraic)
        )
        lidraughts.mon.export.pdn.game()
        Env.api.gameApiV2.exportOne(game, config) flatMap { content =>
          Env.api.gameApiV2.filename(game, config.format) map { filename =>
            Ok(content).withHeaders(
              CONTENT_TYPE -> gameContentType(config),
              CONTENT_DISPOSITION -> s"attachment; filename=$filename"
            )
          }
        }
      }
    }
  }

  def exportByUser(username: String) = OpenOrScoped()(
    open = ctx => handleExport(username, ctx.me, ctx.req, ctx.pref.draughtsResult, ctx.pref.canAlgebraic, oauth = false),
    scoped = req => me => handleExport(username, me.some, req, defaultPref.draughtsResult, defaultPref.canAlgebraic, oauth = true)
  )

  def apiExportByUser(username: String) = AnonOrScoped()(
    anon = req => handleExport(username, none, req, defaultPref.draughtsResult, defaultPref.canAlgebraic, oauth = false),
    scoped = req => me => handleExport(username, me.some, req, defaultPref.draughtsResult, defaultPref.canAlgebraic, oauth = true)
  )

  private def handleExport(username: String, me: Option[lidraughts.user.User], req: RequestHeader, draughtsResult: Boolean, algebraicPref: Boolean, oauth: Boolean) =
    lidraughts.user.UserRepo named username flatMap {
      _ ?? { user =>
        Api.GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
          Api.GlobalLinearLimitPerUserOption(me) {
            val format = GameApiV2.Format byRequest req
            WithVs(req) { vs =>
              val config = GameApiV2.ByUserConfig(
                user = user,
                format = format,
                vs = vs,
                since = getLong("since", req) map { new DateTime(_) },
                until = getLong("until", req) map { new DateTime(_) },
                max = getInt("max", req) map (_ atLeast 1),
                rated = getBoolOpt("rated", req),
                perfType = ~get("perfType", req) split "," flatMap { lidraughts.rating.PerfType(_) } toSet,
                color = get("color", req) flatMap draughts.Color.apply,
                analysed = getBoolOpt("analysed", req),
                ongoing = getBool("ongoing", req),
                flags = requestPdnFlags(req, draughtsResult, extended = false, algebraicPref).copy(
                  literate = false
                ),
                perSecond = MaxPerSecond(me match {
                  case Some(m) if m is user.id => 50
                  case Some(_) if oauth => 20 // bonus for oauth logged in only (not for XSRF)
                  case _ => 10
                })
              )
              val date = DateTimeFormat forPattern "yyyy-MM-dd" print new DateTime
              Ok.chunked(Env.api.gameApiV2.exportByUser(config)).withHeaders(
                noProxyBufferHeader,
                CONTENT_TYPE -> gameContentType(config),
                CONTENT_DISPOSITION -> s"attachment; filename=lidraughts_${user.username}_$date.${format.toString.toLowerCase}"
              ).fuccess
            }
          }
        }
      }
    }

  def exportByIds = Action.async(parse.tolerantText) { req =>
    Api.GlobalLinearLimitPerIP(HTTPRequest lastRemoteAddress req) {
      val config = GameApiV2.ByIdsConfig(
        ids = req.body.split(',').take(300),
        format = GameApiV2.Format byRequest req,
        flags = requestPdnFlags(req, defaultPref.draughtsResult, extended = false, defaultPref.canAlgebraic),
        perSecond = MaxPerSecond(20)
      )
      Ok.chunked(Env.api.gameApiV2.exportByIds(config)).withHeaders(
        noProxyBufferHeader,
        CONTENT_TYPE -> gameContentType(config)
      ).fuccess
    }
  }

  private def WithVs(req: RequestHeader)(f: Option[lidraughts.user.User] => Fu[Result]): Fu[Result] =
    get("vs", req) match {
      case None => f(none)
      case Some(name) => lidraughts.user.UserRepo named name flatMap {
        case None => notFoundJson(s"No such opponent: $name")
        case Some(user) => f(user.some)
      }
    }

  private[controllers] def requestPdnFlags(req: RequestHeader, draughtsResult: Boolean, extended: Boolean, algebraicPref: Boolean) =
    lidraughts.game.PdnDump.WithFlags(
      moves = getBoolOpt("moves", req) | true,
      tags = getBoolOpt("tags", req) | true,
      clocks = getBoolOpt("clocks", req) | extended,
      evals = getBoolOpt("evals", req) | extended,
      opening = getBoolOpt("opening", req) | extended,
      literate = getBoolOpt("literate", req) | false,
      algebraic = getBoolOpt("algebraic", req) | algebraicPref,
      draughtsResult = draughtsResult
    )

  private[controllers] def gameContentType(config: GameApiV2.Config) = config.format match {
    case GameApiV2.Format.PDN => pdnContentType
    case GameApiV2.Format.JSON => config match {
      case _: GameApiV2.OneConfig => JSON
      case _ => ndJsonContentType
    }
  }

  private[controllers] def preloadUsers(game: GameModel): Funit =
    Env.user.lightUserApi preloadMany game.userIds
}
