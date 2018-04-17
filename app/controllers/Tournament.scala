package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.chat.Chat
import lila.common.HTTPRequest
import lila.game.{ Pov, GameRepo }
import lila.tournament.{ System, TournamentRepo, PairingRepo, VisibleTournaments, Tournament => Tour }
import lila.user.{ User => UserModel }
import views._

object Tournament extends LilaController {

  private def env = Env.tournament
  private def repo = TournamentRepo

  private def tournamentNotFound(implicit ctx: Context) = NotFound(html.tournament.notFound())

  private[controllers] val upcomingCache = Env.memo.asyncCache.single[(VisibleTournaments, List[Tour])](
    name = "tournament.home",
    for {
      visible <- env.api.fetchVisibleTournaments
      scheduled <- repo.scheduledDedup
    } yield (visible, scheduled),
    expireAfter = _.ExpireAfterWrite(3 seconds)
  )

  def home(page: Int) = Open { implicit ctx =>
    negotiate(
      html = Reasonable(page, 20) {
        pageHit
        val finishedPaginator = repo.finishedPaginator(lila.common.MaxPerPage(30), page = page)
        if (HTTPRequest isXhr ctx.req) for {
          pag <- finishedPaginator
          _ <- Env.user.lightUserApi preloadMany pag.currentPageResults.flatMap(_.winnerId)
        } yield Ok(html.tournament.finishedPaginator(pag))
        else for {
          (visible, scheduled) <- upcomingCache.get
          finished <- finishedPaginator
          winners <- env.winners.all
          _ <- Env.user.lightUserApi preloadMany {
            finished.currentPageResults.flatMap(_.winnerId).toList :::
              scheduled.flatMap(_.winnerId) ::: winners.userIds
          }
          scheduleJson <- env scheduleJsonView visible
        } yield NoCache {
          Ok(html.tournament.home(scheduled, finished, winners, scheduleJson))
        }
      },
      api = _ => for {
        (visible, _) <- upcomingCache.get
        scheduleJson <- env scheduleJsonView visible
      } yield Ok(scheduleJson)
    )
  }

  def help(sysStr: Option[String]) = Open { implicit ctx =>
    val system = sysStr flatMap {
      case "arena" => System.Arena.some
      case _ => none
    }
    Ok(html.tournament.faqPage(system)).fuccess
  }

  def leaderboard = Open { implicit ctx =>
    for {
      winners <- env.winners.all
      _ <- Env.user.lightUserApi preloadMany winners.userIds
    } yield Ok(html.tournament.leaderboard(winners))
  }

  private[controllers] def canHaveChat(tour: Tour)(implicit ctx: Context): Boolean = ctx.me ?? { u =>
    if (ctx.kid) false
    else if (tour.isPrivate) true
    else Env.chat.panic.allowed(u, tighter = tour.variant == chess.variant.Antichess)
  }

  def show(id: String) = Open { implicit ctx =>
    val page = getInt("page")
    negotiate(
      html = repo byId id flatMap {
        _.fold(tournamentNotFound.fuccess) { tour =>
          (for {
            verdicts <- env.api.verdicts(tour, ctx.me)
            version <- env.version(tour.id)
            chat <- canHaveChat(tour) ?? Env.chat.api.userChat.cached.findMine(Chat.Id(tour.id), ctx.me).map(some)
            json <- env.jsonView(tour, page, ctx.me, none, version.some, ctx.lang)
            _ <- chat ?? { c => Env.user.lightUserApi.preloadMany(c.chat.userIds) }
            streamers <- streamerCache get tour.id
            shieldOwner <- env.shieldApi currentOwner tour
          } yield Ok(html.tournament.show(tour, verdicts, json, chat, streamers, shieldOwner))).mon(_.http.response.tournament.show.website)
        }
      },
      api = _ => repo byId id flatMap {
        case None => NotFound(jsonError("No such tournament")).fuccess
        case Some(tour) => {
          get("playerInfo").?? { env.api.playerInfo(tour.id, _) } zip
            getBool("socketVersion").??(env version tour.id map some) flatMap {
              case (playerInfoExt, socketVersion) =>
                env.jsonView(tour, page, ctx.me, playerInfoExt, socketVersion, ctx.lang)
            } map { Ok(_) }
        }.mon(_.http.response.tournament.show.mobile)
      } map (_ as JSON)
    ) map NoCache
  }

  def standing(id: String, page: Int) = Open { implicit ctx =>
    OptionFuResult(repo byId id) { tour =>
      env.jsonView.standing(tour, page) map { data =>
        Ok(data) as JSON
      }
    }
  }

  def pageOf(id: String, userId: String) = Open { implicit ctx =>
    OptionFuResult(repo byId id) { tour =>
      env.api.pageOf(tour, UserModel normalize userId) flatMap {
        _ ?? { page =>
          env.jsonView.standing(tour, page) map { data =>
            Ok(data) as JSON
          }
        }
      }
    }
  }

  def userGameNbMini(id: String, user: String, nb: Int) = Open { implicit ctx =>
    withUserGameNb(id, user, nb) { pov =>
      Ok(html.tournament.miniGame(pov))
    }
  }

  def userGameNbShow(id: String, user: String, nb: Int) = Open { implicit ctx =>
    withUserGameNb(id, user, nb) { pov =>
      Redirect(routes.Round.watcher(pov.gameId, pov.color.name))
    }
  }

  private def withUserGameNb(id: String, user: String, nb: Int)(withPov: Pov => Result)(implicit ctx: Context): Fu[Result] = {
    val userId = lila.user.User normalize user
    OptionFuResult(PairingRepo.byTourUserNb(id, userId, nb)) { pairing =>
      GameRepo game pairing.id map {
        _.flatMap { Pov.ofUserId(_, userId) }.fold(Redirect(routes.Tournament show id))(withPov)
      }
    }
  }

  def player(id: String, userId: String) = Open { implicit ctx =>
    JsonOk {
      env.api.playerInfo(id, userId) flatMap {
        _ ?? env.jsonView.playerInfo
      }
    }
  }

  def join(id: String) = AuthBody(BodyParsers.parse.json) { implicit ctx => implicit me =>
    NoLameOrBot {
      NoPlayban {
        val password = ctx.body.body.\("p").asOpt[String]
        negotiate(
          html = repo enterableById id map {
            case None => tournamentNotFound
            case Some(tour) =>
              env.api.join(tour.id, me, password)
              Redirect(routes.Tournament.show(tour.id))
          },
          api = _ => OptionFuResult(repo enterableById id) { tour =>
            env.api.joinWithResult(tour.id, me, password) map { result =>
              if (result) jsonOkResult
              else BadRequest(Json.obj("joined" -> false))
            }
          }
        )
      }
    }
  }

  def pause(id: String) = Auth { implicit ctx => me =>
    OptionResult(repo byId id) { tour =>
      env.api.selfPause(tour.id, me.id)
      if (HTTPRequest.isXhr(ctx.req)) jsonOkResult
      else Redirect(routes.Tournament.show(tour.id))
    }
  }

  def terminate(id: String) = Secure(_.TerminateTournament) { implicit ctx => me =>
    OptionResult(repo byId id) { tour =>
      env.api kill tour
      Env.mod.logApi.terminateTournament(me.id, tour.fullName)
      Redirect(routes.Tournament show tour.id)
    }
  }

  def form = Auth { implicit ctx => me =>
    NoLameOrBot {
      Ok(html.tournament.form(env.forms(me), env.forms, me)).fuccess
    }
  }

  private val CreateLimitPerUser = new lila.memo.RateLimit[lila.user.User.ID](
    credits = 8,
    duration = 24 hour,
    name = "tournament per user",
    key = "tournament.user"
  )

  private val CreateLimitPerIP = new lila.memo.RateLimit[lila.common.IpAddress](
    credits = 8,
    duration = 24 hour,
    name = "tournament per IP",
    key = "tournament.ip"
  )

  private implicit val rateLimited = ornicar.scalalib.Zero.instance[Fu[Result]] {
    fuccess(Redirect(routes.Tournament.home(1)))
  }

  def create = AuthBody { implicit ctx => implicit me =>
    NoLameOrBot {
      implicit val req = ctx.body
      negotiate(
        html = env.forms(me).bindFromRequest.fold(
          err => BadRequest(html.tournament.form(err, env.forms, me)).fuccess,
          setup =>
            CreateLimitPerUser(me.id, cost = 1) {
              CreateLimitPerIP(HTTPRequest lastRemoteAddress ctx.req, cost = 1) {
                env.api.createTournament(setup, me) map { tour =>
                  Redirect(routes.Tournament.show(tour.id))
                }
              }
            }
        ),
        api = _ => env.forms(me).bindFromRequest.fold(
          err => BadRequest(errorsAsJson(err)).fuccess,
          setup => env.api.createTournament(setup, me) map { tour =>
            Ok(Json.obj("id" -> tour.id))
          }
        )
      )
    }
  }

  def limitedInvitation = Auth { implicit ctx => me =>
    for {
      (tours, _) <- upcomingCache.get
      res <- lila.tournament.TournamentInviter.findNextFor(me, tours, env.verify.canEnter(me))
    } yield res.fold(Redirect(routes.Tournament.home(1))) { t =>
      Redirect(routes.Tournament.show(t.id))
    }
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    getSocketUid("sri") ?? { uid =>
      env.socketHandler.join(id, uid, ctx.me)
    }
  }

  def featured = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ =>
        Env.tournament.cached.promotable.get.nevermind map {
          lila.tournament.Spotlight.select(_, ctx.me, 4)
        } flatMap env.scheduleJsonView.featured map { Ok(_) }
    )
  }

  def shields = Open { implicit ctx =>
    for {
      history <- env.shieldApi.history
      _ <- Env.user.lightUserApi preloadMany history.userIds
    } yield html.tournament.shields(history)
  }

  def calendar = Open { implicit ctx =>
    env.api.calendar map { tours =>
      Ok(html.tournament.calendar(env.scheduleJsonView calendar tours))
    }
  }

  private val streamerCache = Env.memo.asyncCache.multi[Tour.ID, Set[UserModel.ID]](
    name = "tournament.streamers",
    f = tourId => Env.streamer.liveStreamApi.all.flatMap {
      _.streams.map { stream =>
        env.hasUser(tourId, stream.streamer.userId) map (_ option stream.streamer.userId)
      }.sequenceFu.map(_.flatten.toSet)
    },
    expireAfter = _.ExpireAfterWrite(15.seconds)
  )
}
