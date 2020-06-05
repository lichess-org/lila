package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import lidraughts.api.{ Context, GameApiV2 }
import lidraughts.app._
import lidraughts.chat.Chat
import lidraughts.common.HTTPRequest
import lidraughts.hub.lightTeam._
import lidraughts.game.GameRepo
import lidraughts.game.PdnDump.WithFlags
import lidraughts.simul.{ Simul => Sim }
import lidraughts.simul.SimulForm.{ empty => emptyForm }
import views._

object Simul extends LidraughtsController {

  private def env = Env.simul
  private def forms = lidraughts.simul.SimulForm

  private def simulNotFound(implicit ctx: Context) = NotFound(html.simul.bits.notFound())

  private val settleResultOptions = Set("hostwin", "hostloss", "draw")

  val home = Open { implicit ctx =>
    pageHit
    fetchSimuls(ctx.me) flatMap {
      case pending ~ created ~ started ~ finished =>
        Ok(html.simul.home(pending, created, started, finished)).fuccess
    }
  }

  val apiList = Action.async {
    fetchSimuls(none) flatMap {
      case pending ~ created ~ started ~ finished =>
        env.jsonView.apiAll(pending, created, started, finished) map { json =>
          Ok(json) as JSON
        }
    }
  }

  val homeReload = Open { implicit ctx =>
    fetchSimuls(ctx.me) map {
      case pending ~ created ~ started ~ finished =>
        Ok(html.simul.homeInner(pending, created, started, finished))
    }
  }

  private def fetchSimuls(me: Option[lidraughts.user.User]) =
    me.?? { u =>
      env.repo.findPending(u.id)
    } zip
      env.allCreatedFeaturable.get zip
      env.repo.allStarted zip
      env.repo.allFinished(30)

  def show(id: String) = Open { implicit ctx =>
    env.repo find id flatMap {
      _.fold(simulNotFound.fuccess) { sim =>
        for {
          team <- sim.team ?? Env.team.api.team
          version <- env.version(sim.id)
          json <- env.jsonView(sim, sim.canHaveCevalUser(ctx.me), ctx.pref.some, team.map { t =>
            lidraughts.simul.SimulTeam(t.id, t.name, ctx.userId exists {
              Env.team.api.syncBelongsTo(t.id, _)
            })
          })
          chat <- canHaveChat(sim) ?? Env.chat.api.userChat.cached.findMine(Chat.Id(sim.id), ctx.me).map(some)
          _ <- chat ?? { c => Env.user.lightUserApi.preloadMany(c.chat.userIds) }
          stream <- Env.streamer.liveStreamApi one sim.hostId
        } yield html.simul.show(sim, version, json, chat, stream, team)
      }
    } map NoCache
  }

  private[controllers] def canHaveChat(sim: Sim)(implicit ctx: Context): Boolean =
    !ctx.kid && // no public chats for kids
      ctx.me.fold(sim.canHaveChat(none)) { u => // anons depend on simul rules
        Env.chat.panic.allowed(u) && sim.canHaveChat(u.some)
      }

  def hostPing(simulId: String) = Open { implicit ctx =>
    AsHostOnly(simulId) { simul =>
      env.repo setHostSeenNow simul
      jsonOkResult
    }
  }

  def start(simulId: String) = Open { implicit ctx =>
    AsHostOrArbiter(simulId) { simul =>
      env.api start simul.id
      jsonOkResult
    }
  }

  def abort(simulId: String) = Open { implicit ctx =>
    AsHostOrArbiter(simulId) { simul =>
      env.api abort simul.id
      jsonOkResult
    }
  }

  def accept(simulId: String, userId: String) = Open { implicit ctx =>
    AsHostOrArbiter(simulId) { simul =>
      env.api.accept(simul.id, userId, true)
      jsonOkResult
    }
  }

  def reject(simulId: String, userId: String) = Open { implicit ctx =>
    AsHostOrArbiter(simulId) { simul =>
      env.api.accept(simul.id, userId, false)
      jsonOkResult
    }
  }

  def setText(simulId: String) = OpenBody { implicit ctx =>
    AsHostOrArbiter(simulId) { simul =>
      implicit val req = ctx.body
      forms.setText.bindFromRequest.fold(
        err => BadRequest,
        text => {
          env.api.setText(simul.id, text)
          jsonOkResult
        }
      )
    }
  }

  def allow(simulId: String, userId: String) = Open { implicit ctx =>
    AsHostOrArbiter(simulId) { simul =>
      env.api.allow(simul.id, userId.toLowerCase, true)
      Ok(Json.obj("ok" -> true)) as JSON
    }
  }

  def disallow(simulId: String, userId: String) = Open { implicit ctx =>
    AsHostOrArbiter(simulId) { simul =>
      env.api.allow(simul.id, userId.toLowerCase, false)
      Ok(Json.obj("ok" -> true)) as JSON
    }
  }

  def allowed(simulId: String) = Open { implicit ctx =>
    AsHostOrArbiter(simulId) { simul =>
      Ok(Json.obj("ok" -> ~simul.allowed)) as JSON
    }
  }

  def settle(simulId: String, userId: String, result: String) = Open { implicit ctx =>
    AsArbiterOnly(simulId) { simul =>
      if (simul.hasPairing(userId) && settleResultOptions.contains(result)) {
        env.api.settle(simul.id, userId, result)
        fuccess(Ok(Json.obj("ok" -> true)) as JSON)
      } else fuccess(BadRequest)
    }
  }

  def arbiter(simulId: String) = Open { implicit ctx =>
    AsArbiterOnly(simulId) { sim =>
      env.jsonView.arbiterJson(sim) map { Ok(_) as JSON }
    }
  }

  def timeOutGame(simulId: String, gameId: String, seconds: Int) = Open { implicit ctx =>
    AsHostOnly(simulId) { simul =>
      simul.pairings.find(p => p.gameId == gameId && p.ongoing) map {
        case pairing if seconds == 0 =>
          GameRepo.unsetTimeOut(pairing.gameId)
          Ok(Json.obj("ok" -> true)) as JSON
        case pairing if seconds > 0 && seconds <= 600 =>
          GameRepo.setTimeOut(pairing.gameId, seconds)
          Ok(Json.obj("ok" -> true)) as JSON
        case _ => BadRequest
      } getOrElse BadRequest
    }
  }

  def form = Auth { implicit ctx => me =>
    NoLameOrBot {
      teamsIBelongTo(me) map { teams =>
        Ok(html.simul.form(forms.create(me), teams, me))
      }
    }
  }

  def create = AuthBody { implicit ctx => implicit me =>
    NoLameOrBot {
      implicit val req = ctx.body
      forms
        .create(me)
        .bindFromRequest
        .fold(
          err => teamsIBelongTo(me) map { teams =>
            BadRequest(html.simul.form(
              forms.applyVariants.bindFromRequest.fold(
                err2 => err,
                data => err.copy(value = emptyForm(me).copy(variants = data.variants).some)
              ),
              teams,
              me
            ))
          },
          setup => env.api.create(setup, me) map { simul =>
            Redirect(routes.Simul.show(simul.id))
          }
        )
    }
  }

  def join(id: String, variant: String) = Auth { implicit ctx => implicit me =>
    NoLameOrBot {
      fuccess {
        env.api.addApplicant(id, me, variant)
        if (HTTPRequest isXhr ctx.req) Ok(Json.obj("ok" -> true)) as JSON
        else Redirect(routes.Simul.show(id))
      }
    }
  }

  def withdraw(id: String) = Auth { implicit ctx => me =>
    fuccess {
      env.api.removeApplicant(id, me)
      if (HTTPRequest isXhr ctx.req) Ok(Json.obj("ok" -> true)) as JSON
      else Redirect(routes.Simul.show(id))
    }
  }

  def exportGames(id: String) = Auth { implicit ctx => me =>
    env.repo.find(id) flatMap {
      case Some(simul) if simul.isFinished =>
        streamGamesPdn(me, id, GameApiV2.ByIdsConfig(
          ids = simul.gameIds,
          format = GameApiV2.Format.PDN,
          flags = WithFlags(draughtsResult = ctx.pref.draughtsResult, algebraic = ctx.pref.canAlgebraic),
          perSecond = lidraughts.common.MaxPerSecond(20)
        )).fuccess
      case _ => fuccess(BadRequest)
    }
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    getSocketUid("sri") ?? { uid =>
      env.socketHandler.join(id, uid, ctx.me, getSocketVersion, apiVersion)
    }
  }

  private val ExportRateLimitPerUser = new lidraughts.memo.RateLimit[lidraughts.user.User.ID](
    credits = 20,
    duration = 1 hour,
    name = "simul export per user",
    key = "simul_export.user"
  )

  private def streamGamesPdn(user: lidraughts.user.User, simulId: String, config: GameApiV2.ByIdsConfig) =
    ExportRateLimitPerUser(user.id, cost = 1) {
      Ok.chunked(Env.api.gameApiV2.exportByIds(config)).withHeaders(
        CONTENT_TYPE -> gameContentType(config),
        CONTENT_DISPOSITION -> ("attachment; filename=" + s"lidraughts_simul_$simulId.${config.format.toString.toLowerCase}")
      )
    }

  private def AsHostOrArbiter(simulId: Sim.ID)(f: Sim => Result)(implicit ctx: Context): Fu[Result] =
    env.repo.find(simulId) flatMap {
      case None => notFound
      case Some(simul) if ctx.userId.exists(simul.hostId ==) || ctx.userId.exists(simul.isArbiter) || isGranted(_.ManageSimul) => fuccess(f(simul))
      case _ => fuccess(Unauthorized)
    }

  private def AsHostOnly(simulId: Sim.ID)(f: Sim => Result)(implicit ctx: Context): Fu[Result] =
    env.repo.find(simulId) flatMap {
      case None => notFound
      case Some(simul) if ctx.userId.exists(simul.hostId ==) => fuccess(f(simul))
      case _ => fuccess(Unauthorized)
    }

  private def AsArbiterOnly(simulId: Sim.ID)(f: Sim => Fu[Result])(implicit ctx: Context): Fu[Result] =
    env.repo.find(simulId) flatMap {
      case None => notFound
      case Some(simul) if ctx.userId.exists(simul.isArbiter) => f(simul)
      case _ => fuccess(Unauthorized)
    }

  private def gameContentType(config: GameApiV2.Config) = config.format match {
    case GameApiV2.Format.PDN => pdnContentType
    case GameApiV2.Format.JSON => ndJsonContentType
  }

  private def teamsIBelongTo(me: lidraughts.user.User): Fu[TeamIdsWithNames] =
    Env.team.api.mine(me) map { _.map(t => t._id -> t.name) }
}
