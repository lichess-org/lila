package controllers

import lila._
import views._
import security.Permission
import user.{ GameFilter, User ⇒ UserModel }
import http.Context

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scalaz.effects._

object User extends LilaController {

  def userRepo = env.user.userRepo
  def paginator = env.user.paginator
  def gamePaginator = env.game.paginator
  def forms = user.DataForm
  def eloUpdater = env.user.eloUpdater

  def show(username: String) = showFilter(username, "all", 1)

  def showFilter(username: String, filterName: String, page: Int) = Open { implicit ctx ⇒
    IOptionIOk(userRepo byId username) { u ⇒
      u.enabled.fold(
        env.user.userInfo(u, ctx) map { info ⇒
          val filters = user.GameFilterMenu(info, ctx.me, filterName)
          html.user.show(u, info,
            games = gamePaginator.recentlyCreated(filters.query)(page),
            filters = filters)
        },
        io(html.user.disabled(u)))
    }
  }

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
      IORedirect(forms.bio.bindFromRequest.fold(
        f ⇒ putStrLn(f.errors.toString) map { _ ⇒ routes.User show me.username },
        bio ⇒ userRepo.setBio(me, bio) map { _ ⇒ routes.User show me.username }
      ))
  }

  val close = Auth { implicit ctx ⇒
    me ⇒
      Ok(html.user.close(me))
  }

  val closeConfirm = Auth { ctx ⇒
    me ⇒
      IORedirect {
        userRepo disable me map { _ ⇒
          env.securityStore deleteUsername me.username
          routes.User show me.username
        }
      }
  }

  def engine(username: String) = Secure(Permission.MarkEngine) { _ ⇒
    _ ⇒
      IORedirect {
        for {
          uOption ← userRepo byId username
          _ ← uOption.fold(userRepo.setEngine, io())
          _ ← uOption.filter(_.elo > UserModel.STARTING_ELO).fold(
            u ⇒ eloUpdater.adjust(u, UserModel.STARTING_ELO),
            io())
        } yield routes.User show username
      }
  }

  def mute(username: String) = Secure(Permission.MutePlayer) { _ ⇒
    _ ⇒
      IORedirect {
        userRepo toggleMute username map { _ ⇒ routes.User show username }
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
