package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lidraughts.api.Context
import lidraughts.app._
import lidraughts.chat.Chat
import lidraughts.common.HTTPRequest
import lidraughts.game.{ Pov, GameRepo }
import lidraughts.hub.tournamentTeam._
import lidraughts.tournament.{ System, TournamentRepo, PairingRepo, VisibleTournaments, Tournament => Tour }
import lidraughts.user.{ User => UserModel }
import views._

object Tournament extends LidraughtsController {

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
        val finishedPaginator = repo.finishedPaginator(lidraughts.common.MaxPerPage(15), page = page)
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
    else Env.chat.panic.allowed(u, tighter = false)
  }

  def show(id: String) = Open { implicit ctx =>
    val page = getInt("page")
    repo byId id flatMap { tourOption =>
      negotiate(
        html = tourOption.fold(tournamentNotFound.fuccess) { tour =>
          (for {
            verdicts <- env.api.verdicts(tour, ctx.me, getUserTeamIds)
            version <- env.version(tour.id)
            chat <- canHaveChat(tour) ?? Env.chat.api.userChat.cached.findMine(Chat.Id(tour.id), ctx.me).map(some)
            json <- env.jsonView(tour, page, ctx.me, getUserTeamIds, none, version.some, partial = false, ctx.lang)
            _ <- chat ?? { c => Env.user.lightUserApi.preloadMany(c.chat.userIds) }
            streamers <- streamerCache get tour.id
            shieldOwner <- env.shieldApi currentOwner tour
          } yield Ok(html.tournament.show(tour, verdicts, json, chat, streamers, shieldOwner))).mon(_.http.response.tournament.show.website)
        }, api = _ => tourOption.fold(notFoundJson("No such tournament")) { tour =>
          get("playerInfo").?? { env.api.playerInfo(tour.id, _) } zip
            getBool("socketVersion").??(env version tour.id map some) flatMap {
              case (playerInfoExt, socketVersion) =>
                val partial = getBool("partial")
                lidraughts.mon.tournament.apiShowPartial(partial)()
                env.jsonView(tour, page, ctx.me, getUserTeamIds, playerInfoExt, socketVersion, partial = partial, ctx.lang)
            } map { Ok(_) }
        }.mon(_.http.response.tournament.show.mobile)
      ) map NoCache
    }
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
      Ok(html.tournament.bits.miniGame(pov))
    }
  }

  def userGameNbShow(id: String, user: String, nb: Int) = Open { implicit ctx =>
    withUserGameNb(id, user, nb) { pov =>
      Redirect(routes.Round.watcher(pov.gameId, pov.color.name))
    }
  }

  private def withUserGameNb(id: String, user: String, nb: Int)(withPov: Pov => Result)(implicit ctx: Context): Fu[Result] = {
    val userId = lidraughts.user.User normalize user
    OptionFuResult(PairingRepo.byTourUserNb(id, userId, nb)) { pairing =>
      GameRepo game pairing.id map {
        _.flatMap { Pov.ofUserId(_, userId) }.fold(Redirect(routes.Tournament show id))(withPov)
      }
    }
  }

  def player(id: String, userId: String) = Open { implicit ctx =>
    JsonOk {
      env.api.playerInfo(id, userId) flatMap {
        _ ?? env.jsonView.playerInfoExtended
      }
    }
  }

  def join(id: String) = AuthBody(BodyParsers.parse.json) { implicit ctx => implicit me =>
    NoLameOrBot {
      NoPlayban {
        val password = ctx.body.body.\("p").asOpt[String]
        env.api.joinWithResult(id, me, password, getUserTeamIds) flatMap { result =>
          negotiate(
            html = Redirect(routes.Tournament.show(id)).fuccess,
            api = _ => fuccess {
              if (result) jsonOkResult
              else BadRequest(Json.obj("joined" -> false))
            }
          )
        }
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

  def form = Auth { implicit ctx => me =>
    NoLameOrBot {
      teamsIBelongTo(me) flatMap { teams =>
        Ok(html.tournament.form(env.forms.create(me), env.forms, me, teams)).fuccess
      }
    }
  }

  private val CreateLimitPerUser = new lidraughts.memo.RateLimit[lidraughts.user.User.ID](
    credits = 24,
    duration = 24 hour,
    name = "tournament per user",
    key = "tournament.user"
  )

  private val CreateLimitPerIP = new lidraughts.memo.RateLimit[lidraughts.common.IpAddress](
    credits = 48,
    duration = 24 hour,
    name = "tournament per IP",
    key = "tournament.ip"
  )

  private val rateLimited = ornicar.scalalib.Zero.instance[Fu[Result]] {
    fuccess(Redirect(routes.Tournament.home(1)))
  }

  def create = AuthBody { implicit ctx => me =>
    NoLameOrBot {
      teamsIBelongTo(me) flatMap { teams =>
        implicit val req = ctx.body
        negotiate(
          html = env.forms.create(me).bindFromRequest.fold(
            err => BadRequest(html.tournament.form(err, env.forms, me, teams)).fuccess,
            setup => {
              val cost = if (me.hasTitle ||
                Env.streamer.liveStreamApi.isStreaming(me.id) ||
                isGranted(_.ManageTournament) ||
                me.isVerified ||
                setup.password.isDefined) 1 else 2
              CreateLimitPerUser(me.id, cost) {
                CreateLimitPerIP(HTTPRequest lastRemoteAddress ctx.req, cost = 1) {
                  env.api.createTournament(setup, me, teams, getUserTeamIds) map { tour =>
                    Redirect(routes.Tournament.show(tour.id))
                  }
                }(rateLimited)
              }(rateLimited)
            }
          ),
          api = _ => doApiCreate(me, teams)
        )
      }
    }
  }

  def apiCreate = ScopedBody() { implicit req => me =>
    if (me.isBot || me.lame) notFoundJson("This account cannot create tournaments")
    else teamsIBelongTo(me) flatMap { teams => doApiCreate(me, teams) }
  }

  private def doApiCreate(me: lidraughts.user.User, teams: TeamIdsWithNames)(implicit req: Request[_]): Fu[Result] =
    env.forms.create(me).bindFromRequest.fold(
      jsonFormErrorDefaultLang,
      setup => env.api.createTournament(setup, me, teams, getUserTeamIds) flatMap { tour =>
        Env.tournament.jsonView(tour, none, none, getUserTeamIds, none, none, partial = false, lidraughts.i18n.defaultLang)
      } map { Ok(_) }
    )

  def limitedInvitation = Auth { implicit ctx => me =>
    for {
      (tours, _) <- upcomingCache.get
      res <- lidraughts.tournament.TournamentInviter.findNextFor(me, tours, env.verify.canEnter(me, getUserTeamIds))
    } yield res.fold(Redirect(routes.Tournament.home(1))) { t =>
      Redirect(routes.Tournament.show(t.id))
    }
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    getSocketUid("sri") ?? { uid =>
      env.socketHandler.join(id, uid, ctx.me, getSocketVersion, apiVersion)
    }
  }

  def featured = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ =>
        Env.tournament.cached.promotable.get.nevermind map {
          lidraughts.tournament.Spotlight.select(_, ctx.me, 4)
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

  def edit(id: String) = Auth { implicit ctx => me =>
    WithEditableTournament(id, me) { tour =>
      teamsIBelongTo(me) map { teams =>
        Ok(html.tournament.editForm(tour, env.forms.edit(me, tour), env.forms, me, teams))
      }
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    WithEditableTournament(id, me) { tour =>
      implicit val req = ctx.body
      teamsIBelongTo(me) flatMap { teams =>
        env.forms.edit(me, tour).bindFromRequest
          .fold(
            err => BadRequest(html.tournament.editForm(tour, err, env.forms, me, teams)).fuccess,
            data => env.api.update(tour, data, me, teams) inject Redirect(routes.Tournament.show(id))
          )
      }
    }
  }

  def terminate(id: String) = Auth { implicit ctx => me =>
    WithEditableTournament(id, me) { tour =>
      env.api kill tour
      Env.mod.logApi.terminateTournament(me.id, tour.fullName)
      Redirect(routes.Tournament.home(1)).fuccess
    }
  }

  private def WithEditableTournament(id: String, me: UserModel)(
    f: Tour => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    repo byId id flatMap {
      case Some(t) if (t.createdBy == me.id && t.isCreated) || isGranted(_.ManageTournament) =>
        f(t)
      case Some(t) => Redirect(routes.Tournament.show(t.id)).fuccess
      case _ => notFound
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

  private val teamApi = Env.team.api
  private val teamCached = Env.team.cached
  private def getUserTeamIds(user: lidraughts.user.User): Fu[TeamIdList] =
    teamCached.teamIdsList(user.id)
  private def teamsIBelongTo(me: lidraughts.user.User): Fu[TeamIdsWithNames] =
    teamApi.mine(me) map { teams =>
      teams.map(t => t._id -> t.name)
    }
}
