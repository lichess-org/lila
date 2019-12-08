package controllers

import com.github.ghik.silencer.silent
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.chat.Chat
import lila.common.config.MaxPerPage
import lila.common.HTTPRequest
import lila.hub.LightTeam._
import lila.tournament.{ VisibleTournaments, Tournament => Tour }
import lila.user.{ User => UserModel }
import views._

final class Tournament(
    env: Env,
    teamC: => Team
) extends LilaController(env) {

  private def repo = env.tournament.tournamentRepo
  private def api = env.tournament.api
  private def jsonView = env.tournament.jsonView
  private def forms = env.tournament.forms

  private def tournamentNotFound(implicit ctx: Context) = NotFound(html.tournament.bits.notFound())

  private[controllers] val upcomingCache = env.memo.asyncCache.single[(VisibleTournaments, List[Tour])](
    name = "tournament.home",
    for {
      visible <- api.fetchVisibleTournaments
      scheduled <- repo.scheduledDedup
    } yield (visible, scheduled),
    expireAfter = _.ExpireAfterWrite(3 seconds)
  )

  def home(page: Int) = Open { implicit ctx =>
    negotiate(
      html = Reasonable(page, 20) {
        pageHit
        val finishedPaginator = repo.finishedPaginator(MaxPerPage(15), page = page)
        if (HTTPRequest isXhr ctx.req) for {
          pag <- finishedPaginator
          _ <- env.user.lightUserApi preloadMany pag.currentPageResults.flatMap(_.winnerId)
        } yield Ok(html.tournament.finishedPaginator(pag))
        else for {
          (visible, scheduled) <- upcomingCache.get
          finished <- finishedPaginator
          winners <- env.tournament.winners.all
          _ <- env.user.lightUserApi preloadMany {
            finished.currentPageResults.flatMap(_.winnerId).toList :::
              scheduled.flatMap(_.winnerId) ::: winners.userIds
          }
          scheduleJson <- env.tournament apiJsonView visible
        } yield NoCache {
          Ok(html.tournament.home(scheduled, finished, winners, scheduleJson))
        }
      },
      api = _ => for {
        (visible, _) <- upcomingCache.get
        scheduleJson <- env.tournament apiJsonView visible
      } yield Ok(scheduleJson)
    )
  }

  def help(@silent sysStr: Option[String]) = Open { implicit ctx =>
    Ok(html.tournament.faq.page).fuccess
  }

  def leaderboard = Open { implicit ctx =>
    for {
      winners <- env.tournament.winners.all
      _ <- env.user.lightUserApi preloadMany winners.userIds
    } yield Ok(html.tournament.leaderboard(winners))
  }

  private[controllers] def canHaveChat(tour: Tour, json: Option[JsObject])(implicit ctx: Context): Boolean =
    !ctx.kid && // no public chats for kids
      ctx.me.fold(!tour.isPrivate) { u => // anon can see public chats, except for private tournaments
        (!tour.isPrivate || json.fold(true)(jsonHasMe)) && // private tournament that I joined
          env.chat.panic.allowed(u, tighter = tour.variant == chess.variant.Antichess)
      }

  private def jsonHasMe(js: JsObject): Boolean = (js \ "me").toOption.isDefined

  def show(id: String) = Open { implicit ctx =>
    val page = getInt("page")
    repo byId id flatMap { tourOption =>
      negotiate(
        html = tourOption.fold(tournamentNotFound.fuccess) { tour =>
          (for {
            verdicts <- api.verdicts(tour, ctx.me, getUserTeamIds)
            version <- env.tournament.version(tour.id)
            json <- jsonView(
              tour = tour,
              page = page,
              me = ctx.me,
              getUserTeamIds = getUserTeamIds,
              getTeamName = env.team.getTeamName,
              playerInfoExt = none,
              socketVersion = version.some,
              partial = false,
              lang = ctx.lang
            )
            chat <- canHaveChat(tour, json.some) ?? env.chat.api.userChat.cached.findMine(Chat.Id(tour.id), ctx.me).map(some)
            _ <- chat ?? { c => env.user.lightUserApi.preloadMany(c.chat.userIds) }
            _ <- tour.teamBattle ?? { b => env.team.cached.preloadSet(b.teams) }
            streamers <- streamerCache get tour.id
            shieldOwner <- env.tournament.shieldApi currentOwner tour
          } yield Ok(html.tournament.show(tour, verdicts, json, chat, streamers, shieldOwner)))
        }, api = _ => tourOption.fold(notFoundJson("No such tournament")) { tour =>
          get("playerInfo").?? { api.playerInfo(tour, _) } zip
            getBool("socketVersion").??(env.tournament version tour.id map some) flatMap {
              case (playerInfoExt, socketVersion) =>
                val partial = getBool("partial")
                lila.mon.tournament.apiShowPartial(partial)()
                jsonView(
                  tour = tour,
                  page = page,
                  me = ctx.me,
                  getUserTeamIds = getUserTeamIds,
                  getTeamName = env.team.getTeamName,
                  playerInfoExt = playerInfoExt,
                  socketVersion = socketVersion,
                  partial = partial,
                  lang = ctx.lang
                )
            } dmap { Ok(_) }
        }
      ) dmap NoCache
    }
  }

  def standing(id: String, page: Int) = Open { implicit ctx =>
    OptionFuResult(repo byId id) { tour =>
      jsonView.standing(tour, page) map { data =>
        Ok(data) as JSON
      }
    }
  }

  def pageOf(id: String, userId: String) = Open { implicit ctx =>
    OptionFuResult(repo byId id) { tour =>
      api.pageOf(tour, UserModel normalize userId) flatMap {
        _ ?? { page =>
          jsonView.standing(tour, page) map { data =>
            Ok(data) as JSON
          }
        }
      }
    }
  }

  def player(tourId: String, userId: String) = Open { _ =>
    env.tournament.tournamentRepo byId tourId flatMap {
      _ ?? { tour =>
        JsonOk {
          api.playerInfo(tour, userId) flatMap {
            _ ?? { jsonView.playerInfoExtended(tour, _) }
          }
        }
      }
    }
  }

  def teamInfo(tourId: String, teamId: String) = Open { _ =>
    env.tournament.tournamentRepo byId tourId flatMap {
      _ ?? { tour =>
        jsonView.teamInfo(tour, teamId) map {
          _ ?? { json =>
            Ok(json) as JSON
          }
        }
      }
    }
  }

  def join(id: String) = AuthBody(parse.json) { implicit ctx => implicit me =>
    NoLameOrBot {
      NoPlayban {
        val password = ctx.body.body.\("p").asOpt[String]
        val teamId = ctx.body.body.\("team").asOpt[String]
        api.joinWithResult(id, me, password, teamId, getUserTeamIds) flatMap { result =>
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
      api.selfPause(tour.id, me.id)
      if (HTTPRequest.isXhr(ctx.req)) jsonOkResult
      else Redirect(routes.Tournament.show(tour.id))
    }
  }

  def terminate(id: String) = Secure(_.TerminateTournament) { implicit ctx => me =>
    OptionResult(repo byId id) { tour =>
      api kill tour
      env.mod.logApi.terminateTournament(me.id, tour.fullName)
      Redirect(routes.Tournament show tour.id)
    }
  }

  def form = Auth { implicit ctx => me =>
    NoLameOrBot {
      teamC.teamsIBelongTo(me) map { teams =>
        Ok(html.tournament.form(forms(me), me, teams))
      }
    }
  }

  def teamBattleForm(teamId: String) = Auth { implicit ctx => me =>
    NoLameOrBot {
      env.team.api.owns(teamId, me.id) map {
        _ ?? {
          Ok(html.tournament.form(forms(me, teamId.some), me, Nil))
        }
      }
    }
  }

  private val CreateLimitPerUser = new lila.memo.RateLimit[lila.user.User.ID](
    credits = 12,
    duration = 24 hour,
    name = "tournament per user",
    key = "tournament.user"
  )

  private val CreateLimitPerIP = new lila.memo.RateLimit[lila.common.IpAddress](
    credits = 16,
    duration = 24 hour,
    name = "tournament per IP",
    key = "tournament.ip"
  )

  private val rateLimited = ornicar.scalalib.Zero.instance[Fu[Result]] {
    fuccess(Redirect(routes.Tournament.home(1)))
  }

  def create = AuthBody { implicit ctx => me =>
    NoLameOrBot {
      teamC.teamsIBelongTo(me) flatMap { teams =>
        implicit val req = ctx.body
        negotiate(
          html = forms(me).bindFromRequest.fold(
            err => BadRequest(html.tournament.form(err, me, teams)).fuccess,
            setup => {
              val cost = if (me.hasTitle ||
                env.streamer.liveStreamApi.isStreaming(me.id) ||
                isGranted(_.ManageTournament) ||
                setup.password.isDefined) 1 else 4
              CreateLimitPerUser(me.id, cost = cost) {
                CreateLimitPerIP(HTTPRequest lastRemoteAddress ctx.req, cost = cost) {
                  api.createTournament(setup, me, teams, getUserTeamIds) map { tour =>
                    if (tour.teamBattle.isDefined) Redirect(routes.Tournament.teamBattleEdit(tour.id))
                    else Redirect(routes.Tournament.show(tour.id))
                  }
                }(rateLimited)
              }(rateLimited)
            }
          ),
          api = _ => doApiCreate(me)
        )
      }
    }
  }

  def apiCreate = ScopedBody() { implicit req => me =>
    if (me.isBot || me.lame) notFoundJson("This account cannot create tournaments")
    else doApiCreate(me)
  }

  private def doApiCreate(me: lila.user.User)(implicit req: Request[_]): Fu[Result] =
    forms(me).bindFromRequest.fold(
      jsonFormErrorDefaultLang,
      setup => teamC.teamsIBelongTo(me) flatMap { teams =>
        api.createTournament(setup, me, teams, getUserTeamIds) flatMap { tour =>
          jsonView(tour, none, none, getUserTeamIds, env.team.getTeamName, none, none, partial = false, lila.i18n.defaultLang)
        }
      } map { Ok(_) }
    )

  def teamBattleEdit(id: String) = Auth { implicit ctx => me =>
    repo byId id flatMap {
      _ ?? {
        case tour if tour.createdBy == me.id =>
          tour.teamBattle ?? { battle =>
            env.team.teamRepo.byOrderedIds(battle.sortedTeamIds) flatMap { teams =>
              env.user.lightUserApi.preloadMany(teams.map(_.createdBy)) >> {
                val form = lila.tournament.TeamBattle.DataForm.edit(teams.map { t =>
                  s"""${t.id} "${t.name}" by ${env.user.lightUserApi.sync(t.createdBy).fold(t.createdBy)(_.name)}"""
                }, battle.nbLeaders)
                Ok(html.tournament.teamBattle.edit(tour, form)).fuccess
              }
            }
          }
        case tour => Redirect(routes.Tournament.show(tour.id)).fuccess
      }
    }
  }

  def teamBattleUpdate(id: String) = AuthBody { implicit ctx => me =>
    repo byId id flatMap {
      _ ?? {
        case tour if tour.createdBy == me.id && !tour.isFinished =>
          implicit val req = ctx.body
          lila.tournament.TeamBattle.DataForm.empty.bindFromRequest.fold(
            err => BadRequest(html.tournament.teamBattle.edit(tour, err)).fuccess,
            res => api.teamBattleUpdate(tour, res, env.team.api.filterExistingIds) inject
              Redirect(routes.Tournament.show(tour.id))
          )
        case tour => Redirect(routes.Tournament.show(tour.id)).fuccess
      }
    }
  }

  def featured = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ =>
        env.tournament.cached.promotable.get.nevermind map {
          lila.tournament.Spotlight.select(_, ctx.me, 4)
        } flatMap env.tournament.apiJsonView.featured map { Ok(_) }
    )
  }

  def shields = Open { implicit ctx =>
    for {
      history <- env.tournament.shieldApi.history(5.some)
      _ <- env.user.lightUserApi preloadMany history.userIds
    } yield html.tournament.shields(history)
  }

  def categShields(k: String) = Open { implicit ctx =>
    OptionFuOk(env.tournament.shieldApi.byCategKey(k)) {
      case (categ, awards) =>
        env.user.lightUserApi preloadMany awards.map(_.owner.value) inject
          html.tournament.shields.byCateg(categ, awards)
    }
  }

  def calendar = Open { implicit ctx =>
    api.calendar map { tours =>
      Ok(html.tournament.calendar(env.tournament.apiJsonView calendar tours))
    }
  }

  private val streamerCache = env.memo.asyncCache.multi[Tour.ID, Set[UserModel.ID]](
    name = "tournament.streamers",
    f = tourId => env.streamer.liveStreamApi.all.flatMap {
      _.streams.map { stream =>
        env.tournament.hasUser(tourId, stream.streamer.userId) map (_ option stream.streamer.userId)
      }.sequenceFu.map(_.flatten.toSet)
    },
    expireAfter = _.ExpireAfterWrite(15.seconds)
  )

  private def getUserTeamIds(user: lila.user.User): Fu[List[TeamID]] =
    env.team.cached.teamIdsList(user.id)
}
