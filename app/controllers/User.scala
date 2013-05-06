package controllers

import lila.app._
import views._
import lila.security.Permission
import lila.user.{ Context, User ⇒ UserModel, UserRepo }
import lila.common.LilaCookie

import play.api.mvc._, Results._

object User extends LilaController {

  private def paginator = Env.user.paginator
  // private def gamePaginator = env.game.paginator
  private def forms = lila.user.DataForm
  private def eloUpdater = Env.user.eloUpdater
  // private def bookmarkApi = env.bookmark.api
  // private def modApi = env.mod.api

  def show(username: String) = Open { implicit ctx =>
    UserRepo named username map { userOption =>
      println(userOption)
      NotFound
    }
  }
    //showFilter(username, "all", 1)

  // def showFilter(username: String, filterName: String, page: Int) = Open { implicit ctx ⇒
  //   Async {
  //     Akka.future {
  //       (page < 50).fold(
  //         IOptionIOk(userRepo byId username) { userShow(_, filterName, page) },
  //         BadRequest("too old")
  //       )
  //     }
  //   }
  // }

  // private def userShow(u: UserModel, filterName: String, page: Int)(implicit ctx: Context) =
  //   (u.enabled || isGranted(_.MarkEngine)).fold({
  //     val userSpy = isGranted(_.UserSpy) option securityStore.userSpy _
  //     Env.userInfo(u, bookmarkApi, userSpy, ctx) map { info ⇒
  //       val filters = GameFilterMenu(info, ctx.me, filterName)
  //       val paginator = filters.query.fold(bookmarkApi.gamePaginatorByUser(u, page)) { query ⇒
  //         gamePaginator.recentlyCreated(query, filters.cachedNb)(page)
  //       }
  //       html.user.show(u, info, paginator, filters)
  //     }
  //   }, io(html.user.disabled(u)))

  def list(page: Int) = TODO
  // def list(page: Int) = Open { implicit ctx ⇒
  //   (page < 50).fold(
  //     IOk(onlineUsers map { html.user.list(paginator elo page, _) }),
  //     BadRequest("too old")
  //   )
  // }

  // val online = Open { implicit ctx ⇒
  //   IOk(onlineUsers map { html.user.online(_) })
  // }

  def autocomplete = Open { implicit ctx ⇒
    get("term", ctx.req).filter(""!=).fold(BadRequest("No search term provided").fuccess: Fu[Result]) { term ⇒
      JsonOk(UserRepo usernamesLike term)
    }
  }

  // val getBio = Auth { ctx ⇒ me ⇒ Ok(me.bio) }

  // val setBio = AuthBody { ctx ⇒
  //   me ⇒
  //     implicit val req = ctx.body
  //     JsonIOk(forms.bio.bindFromRequest.fold(
  //       f ⇒ putStrLn(f.errors.toString) map { _ ⇒ me.bio | "" },
  //       bio ⇒ userRepo.setBio(me, bio) map { _ ⇒ bio }
  //     ) map { bio ⇒ Map("bio" -> bio) })
  // }

  // val passwd = Auth { implicit ctx ⇒
  //   me ⇒
  //     Ok(html.user.passwd(me, forms.passwd))
  // }

  // val passwdApply = AuthBody { implicit ctx ⇒
  //   me ⇒
  //     implicit val req = ctx.body
  //     FormIOResult(forms.passwd) { err ⇒
  //       html.user.passwd(me, err)
  //     } { passwd ⇒
  //       for {
  //         ok ← userRepo.checkPassword(me.username, passwd.oldPasswd)
  //         _ ← userRepo.passwd(me, passwd.newPasswd1) map (_ ⇒ ()) doIf ok
  //       } yield ok.fold(
  //         Redirect(routes.User show me.username),
  //         BadRequest(html.user.passwd(me, forms.passwd))
  //       )
  //     }
  // }

  // val close = Auth { implicit ctx ⇒
  //   me ⇒
  //     Ok(html.user.close(me))
  // }

  // val closeConfirm = Auth { ctx ⇒
  //   me ⇒
  //   implicit val req = ctx.req
  //     IOResult {
  //       (userRepo disable me) >> 
  //       env.team.api.quitAll(me.id) >>
  //       (Env.security.store deleteUsername me.username) inject { 
  //         Redirect(routes.User show me.username) withCookies LilaCookie.newSession
  //       }
  //     }
  // }

  // def export(username: String) = Open { implicit ctx ⇒
  //   IOptionIOResult(userRepo byId username) { u ⇒
  //     env.game.export(u).apply map { path ⇒
  //       Redirect(path)
  //     }
  //   }
  // }

  // private val onlineUsers: IO[List[UserModel]] =
  //   userRepo byIds Env.user.usernameMemo.keys
}
