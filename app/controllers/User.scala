package controllers

import play.api.mvc._, Results._

import lila.app._
import lila.common.LilaCookie
import lila.db.api.$find
import lila.security.Permission
import lila.user.tube.userTube
import lila.user.{ Context, User ⇒ UserModel, UserRepo }
import views._

object User extends LilaController {

  private def env = Env.user
  private def gamePaginator = Env.game.paginator
  private def forms = lila.user.DataForm

  def show(username: String) = Open { implicit ctx ⇒
    filter(username, "all", 1)
  }

  def showMini(username: String) = Open { implicit ctx ⇒
    mini(username)
  }

  def showFilter(username: String, filterName: String, page: Int) = Open { implicit ctx ⇒
    filter(username, filterName, page)
  }

  def online = Open { implicit req ⇒
    UserRepo.byIdsSortRating(env.onlineUserIdMemo.keys, 1000) map { users ⇒
      html.user.online(users)
    }
  }

  private def filter(username: String, filterName: String, page: Int)(implicit ctx: Context) =
    Reasonable(page) {
      OptionFuResult(UserRepo named username) { u ⇒
        (u.enabled || isGranted(_.UserSpy)).fold({
          userShow(u, filterName, page) map { Ok(_) }
        }, UserRepo isArtificial u.id map { artificial ⇒
          NotFound(html.user.disabled(u, artificial))
        })
      }
    }

  private def userShow(u: UserModel, filterName: String, page: Int)(implicit ctx: Context) = for {
    info ← Env.current.userInfo(u, ctx)
    filters = mashup.GameFilterMenu(info, ctx.me, filterName)
    pag ← (filters.query.fold(Env.bookmark.api.gamePaginatorByUser(u, page)) { query ⇒
      gamePaginator.recentlyCreated(query, filters.cachedNb)(page)
    })
  } yield html.user.show(u, info, pag, filters)

  private def mini(username: String)(implicit ctx: Context) =
    OptionOk(UserRepo named username) { user ⇒
      Thread sleep 200
      html.user.mini(user)
    }

  def list(page: Int) = Open { implicit ctx ⇒
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
        active ← env.cached.topNbGame(nb) map2 { (user: UserModel) ⇒
          user -> user.count.game
        }
        activeDay ← Env.game.cached.activePlayerUidsDay(nb) flatMap { pairs ⇒
          UserRepo.byOrderedIds(pairs.map(_._1)) map (_ zip pairs.map(_._2))
        }
        activeWeek ← Env.game.cached.activePlayerUidsWeek(nb) flatMap { pairs ⇒
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

  def mod(username: String) = Secure(_.UserSpy) { implicit ctx ⇒
    me ⇒ OptionFuOk(UserRepo named username) { user ⇒
      Env.security userSpy user.id map { spy ⇒
        html.user.mod(user, spy)
      }
    }
  }

  def opponents(username: String) = Open { implicit ctx ⇒
    OptionFuOk(UserRepo named username) { user ⇒
      lila.game.BestOpponents(user.id, 50) map { ops ⇒
        html.user.opponents(user, ops)
      }
    }
  }

  def autocomplete = Open { implicit ctx ⇒
    get("term", ctx.req).filter(""!=).fold(BadRequest("No search term provided").fuccess: Fu[SimpleResult]) { term ⇒
      JsonOk(UserRepo usernamesLike term)
    }
  }

  def export(username: String) = Open { implicit ctx ⇒
    OptionFuResult(UserRepo named username) { u ⇒
      Env.game export u map { url ⇒
        Redirect(Env.api.Net.Protocol + Env.api.Net.AssetDomain + url)
      }
    }
  }
}
