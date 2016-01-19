package controllers

import play.api.libs.json.Json
import play.api.mvc._, Results._

import lila.api.{ Context, BodyContext }
import lila.app._
import lila.app.mashup.GameFilterMenu
import lila.common.LilaCookie
import lila.db.api.$find
import lila.evaluation.{ PlayerAggregateAssessment }
import lila.game.{ GameRepo, Pov }
import lila.rating.PerfType
import lila.security.Permission
import lila.user.tube.userTube
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
        Env.donation.isDonor(user.id) zip
        (ctx.userId ?? { relationApi.blocks(user.id, _) }) zip
        (ctx.userId ?? { Env.game.crosstableApi(user.id, _) }) zip
        (ctx.isAuth ?? { Env.pref.api.followable(user.id) }) zip
        (ctx.userId ?? { relationApi.relation(_, user.id) }) map {
          case (((((pov, donor), blocked), crosstable), followable), relation) =>
            Ok(html.user.mini(user, pov, blocked, followable, relation, crosstable, donor))
              .withHeaders(CACHE_CONTROL -> "max-age=5")
        }
    }
  }

  def showFilter(username: String, filterName: String, page: Int) = OpenBody { implicit ctx =>
    filter(username, filterName.some, page)
  }

  def online = Open { implicit req =>
    val max = 100
    def get(nb: Int) = UserRepo.byIdsSortRating(env.onlineUserIdMemo.keys, nb)
    negotiate(
      html = env.cached topOnline max map { html.user.online(_, max) },
      api = _ => env.cached topOnline getInt("nb").fold(10)(_ min max) map { list =>
        Ok(Json.toJson(list.map(env.jsonView(_, true))))
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
          } map { status(_) },
          api = _ => userGames(u, filterOption, page) map {
            case (filterName, pag) => Ok(Env.api.userGameApi.filter(filterName, pag))
          })
        else negotiate(
          html = fuccess(NotFound(html.user.disabled(u))),
          api = _ => fuccess(NotFound(Json.obj("error" -> "No such user, or account closed"))))
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
    relation <- ctx.userId ?? { relationApi.relation(_, u.id) }
    notes <- ctx.me ?? { me =>
      relationApi friends me.id flatMap { env.noteApi.get(u, me, _) }
    }
    followable <- ctx.isAuth ?? { Env.pref.api followable u.id }
    blocked <- ctx.userId ?? { relationApi.blocks(u.id, _) }
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
      bullet ← env.cached top10Perf PerfType.Bullet.key
      blitz ← env.cached top10Perf PerfType.Blitz.key
      classical ← env.cached top10Perf PerfType.Classical.key
      chess960 ← env.cached top10Perf PerfType.Chess960.key
      kingOfTheHill ← env.cached top10Perf PerfType.KingOfTheHill.key
      threeCheck ← env.cached top10Perf PerfType.ThreeCheck.key
      antichess <- env.cached top10Perf PerfType.Antichess.key
      atomic <- env.cached top10Perf PerfType.Atomic.key
      horde <- env.cached top10Perf PerfType.Horde.key
      racingKings <- env.cached top10Perf PerfType.RacingKings.key
      crazyhouse <- env.cached top10Perf PerfType.Crazyhouse.key
      nbAllTime ← env.cached topNbGame nb
      nbDay ← Env.game.cached activePlayerUidsDay nb map {
        _ flatMap { pair =>
          env lightUser pair.userId map { UserModel.LightCount(_, pair.nb) }
        }
      }
      tourneyWinners ← Env.tournament.winners scheduled nb
      online ← env.cached topOnline 50
      res <- negotiate(
        html = fuccess(Ok(html.user.list(
          tourneyWinners = tourneyWinners,
          online = online,
          bullet = bullet,
          blitz = blitz,
          classical = classical,
          chess960 = chess960,
          kingOfTheHill = kingOfTheHill,
          threeCheck = threeCheck,
          antichess = antichess,
          atomic = atomic,
          horde = horde,
          racingKings = racingKings,
          crazyhouse = crazyhouse,
          nbDay = nbDay,
          nbAllTime = nbAllTime))),
        api = _ => fuccess {
          implicit val lightPerfWrites = play.api.libs.json.Writes[UserModel.LightPerf] { l =>
            Json.obj(
              "id" -> l.user.id,
              "username" -> l.user.name,
              "title" -> l.user.title,
              "perfs" -> Json.obj(
                l.perfKey -> Json.obj("rating" -> l.rating, "progress" -> l.progress)))
          }
          Ok(Json.obj(
            "bullet" -> bullet,
            "blitz" -> blitz,
            "classical" -> classical,
            "crazyhouse" -> crazyhouse,
            "chess960" -> chess960,
            "kingOfTheHill" -> kingOfTheHill,
            "threeCheck" -> threeCheck,
            "antichess" -> antichess,
            "atomic" -> atomic,
            "horde" -> horde,
            "racingKings" -> racingKings))
        })
    } yield res
  }

  def top200(perfKey: String) = Open { implicit ctx =>
    lila.rating.PerfType(perfKey).fold(notFound) { perfType =>
      env.cached top200Perf perfType.key map { users =>
        Ok(html.user.top200(perfType, users))
      }
    }
  }

  def mod(username: String) = Secure(_.UserSpy) { implicit ctx =>
    me => OptionFuOk(UserRepo named username) { user =>
      (!isGranted(_.SetEmail, user) ?? UserRepo.email(user.id)) zip
        (Env.security userSpy user.id) zip
        (Env.mod.assessApi.getPlayerAggregateAssessmentWithGames(user.id)) zip
        Env.mod.logApi.userHistory(user.id) zip
        Env.mod.callNeural(user) flatMap {
          case ((((email, spy), playerAggregateAssessment), history), neuralResult) =>
            (Env.playban.api bans spy.usersSharingIp.map(_.id)) map { bans =>
              html.user.mod(user, email, spy, playerAggregateAssessment, bans, history, neuralResult)
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
              case ((u, nb), followable) => ctx.userId ?? { myId =>
                relationApi.relation(myId, u.id)
              } map { lila.relation.Related(u, nb, followable, _) }
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
            Env.user.cached.ratingDistribution(perfType.key) map some
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
}
