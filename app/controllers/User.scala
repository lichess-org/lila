package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.BodyContext
import lila.app._
import lila.app.mashup.GameFilterMenu
import lila.common.HTTPRequest
import lila.common.paginator.Paginator
import lila.game.{ GameRepo, Game => GameModel }
import lila.rating.PerfType
import lila.user.{ User => UserModel, UserRepo }
import views._

object User extends LilaController {

  private def env = Env.user
  private def relationApi = Env.relation.api
  private def userGameSearch = Env.gameSearch.userGameSearch

  def tv(username: String) = Open { implicit ctx =>
    OptionFuResult(UserRepo named username) { user =>
      (GameRepo lastPlayedPlaying user) orElse
        (GameRepo lastPlayed user) flatMap {
          _.fold(fuccess(Redirect(routes.User.show(username)))) { pov =>
            Round.watch(pov, userTv = user.some)
          }
        }
    }
  }

  def show(username: String) = OpenBody { implicit ctx =>
    filter(username, none, 1)
  }

  def showMini(username: String) = Open { implicit ctx =>
    OptionFuResult(UserRepo named username) { user =>
      if (user.enabled) for {
        blocked <- ctx.userId ?? { relationApi.fetchBlocks(user.id, _) }
        crosstable <- ctx.userId ?? { Env.game.crosstableApi(user.id, _) }
        followable <- ctx.isAuth ?? { Env.pref.api.followable(user.id) }
        relation <- ctx.userId ?? { relationApi.fetchRelation(_, user.id) }
        res <- negotiate(
          html = GameRepo lastPlayedPlaying user map { pov =>
          Ok(html.user.mini(user, pov, blocked, followable, relation, crosstable))
            .withHeaders(CACHE_CONTROL -> "max-age=5")
        },
          api = _ => {
          import lila.game.JsonView.crosstableWrites
          fuccess(Ok(Json.obj(
            "crosstable" -> crosstable,
            "perfs" -> lila.user.JsonView.perfs(user, user.best8Perfs)
          )))
        })
      } yield res
      else fuccess(Ok(html.user.miniClosed(user)))
    }
  }

  def showFilter(username: String, filterName: String, page: Int) = OpenBody { implicit ctx =>
    filter(username, filterName.some, page)
  }

  def online = Open { implicit req =>
    val max = 50
    negotiate(
      html = notFound,
      api = _ => env.cached.top50Online.get map { list =>
      Ok(Json.toJson(list.take(getInt("nb").fold(10)(_ min max)).map(env.jsonView(_))))
    })
  }

  private def filter(
    username: String,
    filterOption: Option[String],
    page: Int,
    status: Results.Status = Results.Ok)(implicit ctx: BodyContext[_]) =
    Reasonable(page) {
      OptionFuResult(UserRepo named username) { u =>
        if (u.enabled || isGranted(_.UserSpy)) negotiate(
          html = {
            if (lila.common.HTTPRequest.isSynchronousHttp(ctx.req)) userShow(u, filterOption, page)
            else userGames(u, filterOption, page) map {
              case (filterName, pag) => html.user.games(u, pag, filterName)
            }
          }.map { status(_) }.mon(_.http.response.user.show.website),
          api = _ => userGames(u, filterOption, page).flatMap {
            case (filterName, pag) => Env.api.userGameApi.jsPaginator(pag) map { res =>
              Ok(res ++ Json.obj("filter" -> filterName))
            }
          }.mon(_.http.response.user.show.mobile))
        else negotiate(
          html = fuccess(NotFound(html.user.disabled(u))),
          api = _ => fuccess(NotFound(jsonError("No such user, or account closed"))))
      }
    }

  private def userShow(u: UserModel, filterOption: Option[String], page: Int)(implicit ctx: BodyContext[_]) = for {
    info ← Env.current.userInfo(u, ctx)
    filters = GameFilterMenu(info, ctx.me, filterOption)
    pag <- GameFilterMenu.paginatorOf(
      userGameSearch = userGameSearch,
      user = u,
      info = info.some,
      filter = filters.current,
      me = ctx.me,
      page = page)(ctx.body)
    _ <- Env.user.lightUserApi preloadMany pag.currentPageResults.flatMap(_.userIds)
    _ <- Env.tournament.cached.nameCache preloadMany pag.currentPageResults.flatMap(_.tournamentId)
    _ <- Env.team.cached.nameCache preloadMany info.teamIds
    relation <- ctx.userId ?? { relationApi.fetchRelation(_, u.id) }
    notes <- ctx.me ?? { me =>
      relationApi fetchFriends me.id flatMap { env.noteApi.get(u, me, _, isGranted(_.ModNote)) }
    }
    followable <- ctx.isAuth ?? { Env.pref.api followable u.id }
    blocked <- ctx.userId ?? { relationApi.fetchBlocks(u.id, _) }
    searchForm = GameFilterMenu.searchForm(userGameSearch, filters.current)(ctx.body)
  } yield html.user.show(u, info, pag, filters, searchForm, relation, notes, followable, blocked)

  private val UserGamesRateLimitPerIP = new lila.memo.RateLimit(
    credits = 500,
    duration = 10 minutes,
    name = "user games web/mobile per IP",
    key = "user_games.web.ip")

  implicit val userGamesDefault =
    ornicar.scalalib.Zero.instance[Fu[Paginator[GameModel]]](fuccess(Paginator.empty[GameModel]))

  private def userGames(
    u: UserModel,
    filterOption: Option[String],
    page: Int)(implicit ctx: BodyContext[_]): Fu[(String, Paginator[GameModel])] = {
    import lila.app.mashup.GameFilter.{ All, Playing }
    filterOption.fold({
      Env.simul isHosting u.id map (_.fold(Playing, All).name)
    })(fuccess) flatMap { filterName =>
      UserGamesRateLimitPerIP(HTTPRequest lastRemoteAddress ctx.req, cost = page, msg = s"on ${u.username}") {
        lila.mon.http.userGames.cost(page)
        GameFilterMenu.paginatorOf(
          userGameSearch = userGameSearch,
          user = u,
          info = none,
          filter = GameFilterMenu.currentOf(GameFilterMenu.all, filterName),
          me = ctx.me,
          page = page
        )(ctx.body)
      } map { filterName -> _ }
    }
  }

  def list = Open { implicit ctx =>
    val nb = 10
    for {
      leaderboards <- env.cached.leaderboards
      nbAllTime ← env.cached topNbGame nb
      nbDay ← fuccess(Nil)
      // Env.game.cached activePlayerUidsDay nb map {
      //   _ flatMap { pair =>
      //     env lightUser pair.userId map { UserModel.LightCount(_, pair.nb) }
      //   }
      // }
      tourneyWinners ← Env.tournament.winners.all.map(_.top)
      online ← env.cached.top50Online.get
      _ <- Env.user.lightUserApi preloadMany tourneyWinners.map(_.userId)
      res <- negotiate(
        html = fuccess(Ok(html.user.list(
          tourneyWinners = tourneyWinners,
          online = online,
          leaderboards = leaderboards,
          nbDay = nbDay,
          nbAllTime = nbAllTime))),
        api = _ => fuccess {
          implicit val lpWrites = OWrites[UserModel.LightPerf](env.jsonView.lightPerfIsOnline)
          Ok(Json.obj(
            "bullet" -> leaderboards.bullet,
            "blitz" -> leaderboards.blitz,
            "classical" -> leaderboards.classical,
            "crazyhouse" -> leaderboards.crazyhouse,
            "chess960" -> leaderboards.chess960,
            "kingOfTheHill" -> leaderboards.kingOfTheHill,
            "threeCheck" -> leaderboards.threeCheck,
            "antichess" -> leaderboards.antichess,
            "atomic" -> leaderboards.atomic,
            "horde" -> leaderboards.horde,
            "racingKings" -> leaderboards.racingKings))
        })
    } yield res
  }

  def top200(perfKey: String) = Open { implicit ctx =>
    PerfType(perfKey).fold(notFound) { perfType =>
      env.cached top200Perf perfType.id map { users =>
        Ok(html.user.top200(perfType, users))
      }
    }
  }

  def topWeek = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ => env.cached.topWeek(()).map { users =>
      Ok(Json toJson users.map(env.jsonView.lightPerfIsOnline))
    })
  }

  def mod(username: String) = Secure(_.UserSpy) { implicit ctx => me =>
    OptionFuOk(UserRepo named username) { user =>
      for {
        email <- (!isGranted(_.SetEmail, user) ?? UserRepo.email(user.id))
        spy <- Env.security userSpy user.id
        assess <- Env.mod.assessApi.getPlayerAggregateAssessmentWithGames(user.id)
        history <- Env.mod.logApi.userHistory(user.id)
        charges <- Env.plan.api.recentChargesOf(user)
        reports <- Env.report.api.byAndAbout(user, 20)
        pref <- Env.pref.api.getPref(user)
        bans <- Env.playban.api bans spy.usersSharingIp.map(_.id)
        notes <- Env.user.noteApi.byUserIdsForMod(spy.otherUsers.map(_.user.id))
        _ <- Env.user.lightUserApi preloadMany {
          reports.userIds ::: assess.??(_.games).flatMap(_.userIds)
        }
      } yield html.user.mod(user, email, spy, assess, bans, history, charges, reports, pref, notes)
    }
  }

  def writeNote(username: String) = AuthBody { implicit ctx => me =>
    OptionFuResult(UserRepo named username) { user =>
      implicit val req = ctx.body
      env.forms.note.bindFromRequest.fold(
        err => filter(username, none, 1, Results.BadRequest),
        data => env.noteApi.write(user, data.text, me, data.mod && isGranted(_.ModNote)) inject
          Redirect(routes.User.show(username).url + "?note")
      )
    }
  }

  def opponents = Auth { implicit ctx => me =>
    for {
      ops <- Env.game.bestOpponents(me.id)
      followables <- Env.pref.api.followables(ops map (_._1.id))
      relateds <- ops.zip(followables).map {
        case ((u, nb), followable) => relationApi.fetchRelation(me.id, u.id) map {
          lila.relation.Related(u, nb.some, followable, _)
        }
      }.sequenceFu
    } yield html.user.opponents(me, relateds)
  }

  def perfStat(username: String, perfKey: String) = Open { implicit ctx =>
    OptionFuResult(UserRepo named username) { u =>
      if ((u.disabled || (u.lame && !ctx.is(u))) && !isGranted(_.UserSpy)) notFound
      else PerfType(perfKey).fold(notFound) { perfType =>
        for {
          perfStat <- Env.perfStat.get(u, perfType)
          ranks <- Env.user.cached.ranking.getAll(u.id)
          distribution <- u.perfs(perfType).established ?? {
            Env.user.cached.ratingDistribution(perfType) map some
          }
          _ <- Env.user.lightUserApi preloadMany { u.id :: perfStat.userIds.map(_.value) }
          data = Env.perfStat.jsonView(u, perfStat, ranks get perfType.key, distribution)
          response <- negotiate(
            html = Ok(html.user.perfStat(u, ranks, perfType, data)).fuccess,
            api = _ => getBool("graph").?? {
            Env.history.ratingChartApi.singlePerf(u, perfType).map(_.some)
          } map {
            _.fold(data) { graph => data + ("graph" -> graph) }
          } map { Ok(_) }
          )
        } yield response
      }
    }
  }

  def autocomplete = Open { implicit ctx =>
    get("term", ctx.req).filter(_.nonEmpty) match {
      case None => BadRequest("No search term provided").fuccess
      case Some(term) => JsonOk {
        ctx.me.ifTrue(getBool("friend")) match {
          case None => UserRepo usernamesLike term
          case Some(follower) =>
            Env.relation.api.searchFollowedBy(follower, term, 10) flatMap {
              case Nil     => UserRepo usernamesLike term
              case userIds => UserRepo usernamesByIds userIds
            }
        }
      }
    }
  }

  def myself = Auth { ctx => me =>
    fuccess(Redirect(routes.User.show(me.username)))
  }
}
