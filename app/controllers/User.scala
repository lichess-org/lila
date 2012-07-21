package controllers

import lila._
import views._
import security.Permission
import user.{ GameFilter, User ⇒ UserModel }
import http.Context

import play.api.mvc._
import play.api.mvc.Results._
import scalaz.effects._
import play.api.libs.concurrent.Akka

object User extends LilaController {

  def userRepo = env.user.userRepo
  def paginator = env.user.paginator
  def gamePaginator = env.game.paginator
  def forms = user.DataForm
  def eloUpdater = env.user.eloUpdater
  def lobbyMessenger = env.lobby.messenger
  def bookmarkApi = env.bookmark.api
  def securityStore = env.security.store
  def firewall = env.security.firewall
  def modlogApi = env.modlog.api

  def show(username: String) = showFilter(username, "all", 1)

  def showFilter(username: String, filterName: String, page: Int) = Open { implicit ctx ⇒
    Async {
      Akka.future {
        (page < 50).fold(
          IOptionIOk(userRepo byId username) { userShow(_, filterName, page) },
          BadRequest("too old")
        )
      }
    }
  }

  private def userShow(u: UserModel, filterName: String, page: Int)(implicit ctx: Context) =
    (u.enabled || isGranted(_.MarkEngine)).fold({
      val userSpy = isGranted(_.UserSpy) option securityStore.userSpy _
      env.user.userInfo(u, bookmarkApi, userSpy, ctx) map { info ⇒
        val filters = user.GameFilterMenu(info, ctx.me, filterName)
        val paginator = filters.query.fold(
          query ⇒ gamePaginator.recentlyCreated(query)(page),
          bookmarkApi.gamePaginatorByUser(u, page))
        html.user.show(u, info, paginator, filters)
      }
    }, io(html.user.disabled(u)))

  def list(page: Int) = Open { implicit ctx ⇒
    (page < 50).fold(
      IOk(onlineUsers map { html.user.list(paginator elo page, _) }),
      BadRequest("too old")
    )
  }

  val online = Open { implicit ctx ⇒
    IOk(onlineUsers map { html.user.online(_) })
  }

  val autocomplete = Action { implicit req ⇒
    get("term", req).filter(""!=).fold(
      term ⇒ JsonOk((userRepo usernamesLike term).unsafePerformIO),
      BadRequest("No search term provided")
    )
  }

  val getBio = Auth { ctx ⇒ me ⇒ Ok(me.bio) }

  val setBio = AuthBody { ctx ⇒
    me ⇒
      implicit val req = ctx.body
      JsonIOk(forms.bio.bindFromRequest.fold(
        f ⇒ putStrLn(f.errors.toString) map { _ ⇒ me.bio | "" },
        bio ⇒ userRepo.setBio(me, bio) map { _ ⇒ bio }
      ) map { bio ⇒ Map("bio" -> bio) })
  }

  val close = Auth { implicit ctx ⇒
    me ⇒
      Ok(html.user.close(me))
  }

  val closeConfirm = Auth { ctx ⇒
    me ⇒
      IORedirect {
        userRepo disable me map { _ ⇒
          env.security.store deleteUsername me.username
          routes.User show me.username
        }
      }
  }

  def engine(username: String) = Secure(Permission.MarkEngine) { _ ⇒
    me ⇒
      IORedirect {
        for {
          userOption ← userRepo byId username
          _ ← userOption.fold(
            user ⇒ for {
              _ ← eloUpdater adjust user
              _ ← modlogApi.engine(me, user, !user.engine)
            } yield (),
            io())
        } yield routes.User show username
      }
  }

  def mute(username: String) = Secure(Permission.MutePlayer) { _ ⇒
    _ ⇒
      IORedirect {
        lobbyMessenger mute username map { _ ⇒ routes.User show username }
      }
  }

  def ban(username: String) = Secure(Permission.IpBan) { implicit ctx ⇒
    _ ⇒
      IOptionIORedirect(userRepo byId username) { user ⇒
        for {
          spy ← securityStore userSpy username
          _ ← io(spy.ips foreach firewall.blockIp)
          _ ← user.isChatBan.fold(io(), lobbyMessenger mute username)
        } yield routes.User show username
      }
  }

  val stats = todo

  def export(username: String) = Open { implicit ctx ⇒
    IOptionIOResult(userRepo byId username) { u ⇒
      env.game.export(u).apply map { path ⇒
        Redirect(path)
      }
    }
  }

  private val onlineUsers: IO[List[UserModel]] =
    userRepo byIds env.user.usernameMemo.keys
}
