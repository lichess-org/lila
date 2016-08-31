package controllers

import play.api.libs.json._
import play.api.mvc._, Results._

import lila.api.{ Context, BodyContext }
import lila.app._
import lila.app.mashup.GameFilterMenu
import lila.evaluation.{ PlayerAggregateAssessment }
import lila.game.{ GameRepo, Pov }
import lila.rating.PerfType
import lila.user.{ User => UserModel, UserRepo }
import views._

object User extends LilaController {

  private def env = Env.user
  private def gamePaginator = Env.game.paginator
  private def forms = lila.user.DataForm
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
      GameRepo lastPlayedPlaying user zip
        (ctx.userId ?? { relationApi.fetchBlocks(user.id, _) }) zip
        (ctx.userId ?? { Env.game.crosstableApi(user.id, _) }) zip
        (ctx.isAuth ?? { Env.pref.api.followable(user.id) }) zip
        (ctx.userId ?? { relationApi.fetchRelation(_, user.id) }) flatMap {
          case ((((pov, blocked), crosstable), followable), relation) =>
            negotiate(
              html = fuccess {
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
        }
    }
  }

  def showFilter(username: String, filterName: String, page: Int) = OpenBody { implicit ctx =>
    filter(username, filterName.some, page)
  }

  def online = Open { implicit req =>
    val max = 50
    def get(nb: Int) = UserRepo.byIdsSortRating(env.onlineUserIdMemo.keys, nb)
    negotiate(
      html = notFound,
      api = _ => env.cached top50Online true map { list =>
        Ok(Json.toJson(list.take(getInt("nb").fold(10)(_ min max)).map(env.jsonView(_))))
      }
    )
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
            case (filterName, pag) => Env.api.userGameApi.filter(filterName, pag) map {
              Ok(_)
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
    relation <- ctx.userId ?? { relationApi.fetchRelation(_, u.id) }
    notes <- ctx.me ?? { me =>
      relationApi fetchFriends me.id flatMap { env.noteApi.get(u, me, _) }
    }
    followable <- ctx.isAuth ?? { Env.pref.api followable u.id }
    blocked <- ctx.userId ?? { relationApi.fetchBlocks(u.id, _) }
    searchForm = GameFilterMenu.searchForm(userGameSearch, filters.current)(ctx.body)
  } yield html.user.show(u, info, pag, filters, searchForm, relation, notes, followable, blocked)

  private def userGames(u: UserModel, filterOption: Option[String], page: Int)(implicit ctx: BodyContext[_]) = {
    import lila.app.mashup.GameFilter.{ All, Playing }
    filterOption.fold({
      Env.simul isHosting u.id map (_.fold(Playing, All).name)
    })(fuccess) flatMap { filterName =>
      GameFilterMenu.paginatorOf(
        userGameSearch = userGameSearch,
        user = u,
        info = none,
        filter = GameFilterMenu.currentOf(GameFilterMenu.all, filterName),
        me = ctx.me,
        page = page
      )(ctx.body) map { filterName -> _ }
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
      tourneyWinners ← Env.tournament.winners scheduled nb
      online ← env.cached top50Online true
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
    lila.rating.PerfType(perfKey).fold(notFound) { perfType =>
      env.cached top200Perf perfType.id map { users =>
        Ok(html.user.top200(perfType, users))
      }
    }
  }

  def topWeek = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ => env.cached.topWeek(true).map { users =>
        Ok(Json toJson users.map(env.jsonView.lightPerfIsOnline))
      })
  }

  def mod(username: String) = Secure(_.UserSpy) { implicit ctx =>
    me => OptionFuOk(UserRepo named username) { user =>
      (!isGranted(_.SetEmail, user) ?? UserRepo.email(user.id)) zip
        (Env.security userSpy user.id) zip
        (Env.mod.assessApi.getPlayerAggregateAssessmentWithGames(user.id)) zip
        Env.mod.logApi.userHistory(user.id) zip
        Env.plan.api.recentChargesOf(user) zip
        Env.pref.api.getPref(user) flatMap {
          case (((((email, spy), playerAggregateAssessment), history), charges), pref) =>
            (Env.playban.api bans spy.usersSharingIp.map(_.id)) map { bans =>
              html.user.mod(user, email, spy, playerAggregateAssessment, bans, history, charges, pref)
            }
        }
    }
  }

  def writeNote(username: String) = AuthBody { implicit ctx =>
    me => OptionFuResult(UserRepo named username) { user =>
      implicit val req = ctx.body
      env.forms.note.bindFromRequest.fold(
        err => filter(username, none, 1, Results.BadRequest),
        text => env.noteApi.write(user, text, me) inject Redirect(routes.User.show(username).url + "?note")
      )
    }
  }

  def opponents(username: String) = Open { implicit ctx =>
    OptionFuOk(UserRepo named username) { user =>
      lila.game.BestOpponents(user.id, 50) flatMap { ops =>
        ctx.isAuth.fold(
          Env.pref.api.followables(ops map (_._1.id)),
          fuccess(List.fill(50)(true))
        ) flatMap { followables =>
            (ops zip followables).map {
              case ((u, nb), followable) => ctx.userId ?? {
                relationApi.fetchRelation(_, u.id)
              } map { lila.relation.Related(u, nb.some, followable, _) }
            }.sequenceFu map { relateds =>
              html.user.opponents(user, relateds)
            }
          }
      }
    }
  }

  def perfStat(username: String, perfKey: String) = Open { implicit ctx =>
    OptionFuResult(UserRepo named username) { u =>
      if ((u.disabled || (u.lame && !ctx.is(u))) && !isGranted(_.UserSpy)) notFound
      else lila.rating.PerfType(perfKey).fold(notFound) { perfType =>
        for {
          perfStat <- Env.perfStat.get(u, perfType)
          ranks <- Env.user.cached.ranking.getAll(u.id)
          distribution <- u.perfs(perfType).established ?? {
            Env.user.cached.ratingDistribution(perfType) map some
          }
          data = Env.perfStat.jsonView(u, perfStat, ranks get perfType.key, distribution)
          response <- negotiate(
            html = Ok(html.user.perfStat(u, ranks, perfType, data)).fuccess,
            api = _ => Ok(data).fuccess)
        } yield response
      }
    }
  }

  def autocomplete = Open { implicit ctx =>
    get("term", ctx.req).filter(_.nonEmpty).fold(BadRequest("No search term provided").fuccess: Fu[Result]) { term =>
      JsonOk(UserRepo usernamesLike term)
    }
  }

  def myself = Auth { ctx =>
    me =>
      fuccess(Redirect(routes.User.show(me.username)))
  }
}
