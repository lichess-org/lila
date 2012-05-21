package controllers

import lila._
import views._

import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scalaz.effects._

object User extends LilaController {

  def userRepo = env.user.userRepo
  def paginator = env.user.paginator
  def gamePaginator = env.game.paginator

  def show(username: String) = showMode(username, "all", 1)

  def showMode(username: String, mode: String, page: Int) = Open { implicit ctx ⇒
    IOptionIOk(userRepo byUsername username) { user ⇒
      user.enabled.fold(
        env.user.userInfo(user, ctx) map { info ⇒
          html.user.show(
            u = user,
            info = info,
            mode = mode,
            games = gamePaginator.userAll(user, page))
        },
        io(html.user.disabled(user)))
    }
  }

  def list(page: Int) = Open { implicit ctx ⇒
    IOk(onlineUsers map { html.user.list(paginator elo page, _) })
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

  val signUp = TODO

  val stats = TODO

  def export(username: String) = TODO

  val onlineUsers: IO[List[User]] = userRepo byUsernames env.user.usernameMemo.keys
}
