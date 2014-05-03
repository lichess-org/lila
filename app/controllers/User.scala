package controllers

import play.api.mvc._, Results._

import lila.api.Context
import lila.app._
import lila.common.LilaCookie
import lila.db.api.$find
import lila.game.GameRepo
import lila.security.Permission
import lila.user.tube.userTube
import lila.user.{ User => UserModel, UserRepo }
import views._

object User extends LilaController {

  private def env = Env.user
  private def gamePaginator = Env.game.paginator
  private def forms = lila.user.DataForm
  private def relationApi = Env.relation.api

  def show(username: String) = Open { implicit ctx =>
    filter(username, none, 1)
  }

  def showMini(username: String) = Open { implicit ctx =>
    OptionFuResult(UserRepo named username) { user =>
      GameRepo nowPlaying user.id zip
        (ctx.userId ?? { relationApi.blocks(user.id, _) }) zip
        (ctx.userId ?? { relationApi.follows(user.id, _) }) zip
        (ctx.isAuth ?? { Env.pref.api.followable(user.id) }) zip
        (ctx.userId ?? { relationApi.relation(_, user.id) }) map {
          case ((((game, blocked), followed), followable), relation) =>
            Ok(html.user.mini(user, game, blocked, followed, followable, relation))
              .withHeaders(CACHE_CONTROL -> "max-age=60")
        }
    }
  }

  def showFilter(username: String, filterName: String, page: Int) = Open { implicit ctx =>
    filter(username, filterName.some, page)
  }

  def online = Open { implicit req =>
    val max = 1000
    UserRepo.byIdsSortRating(env.onlineUserIdMemo.keys, max) map { html.user.online(_, max) }
  }

  private def filter(
    username: String,
    filterOption: Option[String],
    page: Int,
    status: Results.Status = Results.Ok)(implicit ctx: Context) =
    Reasonable(page) {
      OptionFuResult(UserRepo named username) { u =>
        (u.enabled || isGranted(_.UserSpy)).fold({
          userShow(u, filterOption, page) map { status(_) }
        }, UserRepo isArtificial u.id map { artificial =>
          NotFound(html.user.disabled(u, artificial))
        })
      }
    }

  private def userShow(u: UserModel, filterOption: Option[String], page: Int)(implicit ctx: Context) = for {
    info ← Env.current.userInfo(u, ctx)
    filterName = filterOption | (if (info.nbWithMe > 0) "me" else "all")
    filters = mashup.GameFilterMenu(info, ctx.me, filterName)
    pag ← (filters.query.fold(Env.bookmark.api.gamePaginatorByUser(u, page)) { query =>
      gamePaginator.recentlyCreated(query, filters.cachedNb)(page)
    })
    data <- GameRepo isNowPlaying u.id
    playing <- GameRepo isNowPlaying u.id
    relation <- ctx.userId ?? { relationApi.relation(_, u.id) }
    notes <- ctx.me ?? { me =>
      relationApi friends me.id flatMap { env.noteApi.get(u, me, _) }
    }
    followable <- ctx.isAuth ?? { Env.pref.api followable u.id }
  } yield html.user.show(u, info, pag, filters, playing, relation, notes, followable)

  def list(page: Int) = Open { implicit ctx =>
    Reasonable(page) {
      val nb = 10
      for {
        progressDay ← env.cached.topProgressDay(nb)
        progressWeek ← env.cached.topProgressWeek(nb)
        progressMonth ← env.cached.topProgressMonth(nb)
        rating ← env.cached.topRating(nb)
        ratingDay ← env.cached.topRatingDay(nb)
        ratingWeek ← env.cached.topRatingWeek(nb)
        online ← env.cached.topOnline(30)
        bullet ← env.cached.topBullet(nb)
        blitz ← env.cached.topBlitz(nb)
        slow ← env.cached.topSlow(nb)
        active ← env.cached.topNbGame(nb) map2 { (user: UserModel) =>
          user -> user.count.game
        }
        activeDay ← Env.game.cached.activePlayerUidsDay(nb) flatMap { pairs =>
          UserRepo.byOrderedIds(pairs.map(_._1)) map (_ zip pairs.map(_._2))
        }
        activeWeek ← Env.game.cached.activePlayerUidsWeek(nb) flatMap { pairs =>
          UserRepo.byOrderedIds(pairs.map(_._1)) map (_ zip pairs.map(_._2))
        }
      } yield html.user.list(
        progressDay = progressDay,
        progressWeek = progressWeek,
        progressMonth = progressMonth,
        rating = rating,
        ratingDay = ratingDay,
        ratingWeek = ratingWeek,
        online = online,
        bullet = bullet,
        blitz = blitz,
        slow = slow,
        nb = active,
        nbDay = activeDay,
        nbWeek = activeWeek)
    }
  }

  def leaderboard = Open { implicit ctx =>
    UserRepo topRating 500 map { users =>
      html.user.leaderboard(users)
    }
  }

  def mod(username: String) = Secure(_.UserSpy) { implicit ctx =>
    me => OptionFuOk(UserRepo named username) { user =>
      Env.evaluation.evaluator find user zip
        (Env.security userSpy user.id) map {
          case (eval, spy) => html.user.mod(user, spy, eval)
        }
    }
  }

  def evaluate(username: String) = Secure(_.UserEvaluate) { implicit ctx =>
    me => OptionFuResult(UserRepo named username) { user =>
      Env.evaluation.evaluator.generate(user, true) inject Redirect(routes.User.show(username).url + "?mod")
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
        (ctx.isAuth ?? { Env.pref.api.followables(ops map (_._1.id)) }) flatMap { followables =>
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

  def autocomplete = Open { implicit ctx =>
    get("term", ctx.req).filter(_.nonEmpty).fold(BadRequest("No search term provided").fuccess: Fu[SimpleResult]) { term =>
      JsonOk(UserRepo usernamesLike term)
    }
  }

  def export(username: String) = Open { implicit ctx =>
    OptionFuResult(UserRepo named username) { u =>
      Env.game export u map { url =>
        Redirect(Env.api.Net.Protocol + Env.api.Net.AssetDomain + url)
      }
    }
  }
}
