package lila.app
package templating

import lila.user.{ User, Users }
import lila.user.Env.{ current ⇒ userEnv }

import controllers.routes

import play.api.templates.Html

trait UserHelper {

  import userEnv._

  def userIdToUsername(userId: String): String =
    cached usernameOrAnonymous userId

  def userIdToUsername(userId: Option[String]): String =
    userId.fold(Users.anonymous)(cached.usernameOrAnonymous)

  def isUsernameOnline(username: String) = usernameMemo get username

  def userIdLink(
    userId: Option[String],
    cssClass: Option[String] = None,
    withOnline: Boolean = true): Html = Html {
    (userId flatMap cached.username).fold(Users.anonymous) { username ⇒
      """<a class="user_link%s%s" href="%s">%s</a>""".format(
        withOnline.fold(
          isUsernameOnline(username).fold(" online", " offline"),
          ""),
        ~cssClass.map(" " + _),
        routes.User.show(username),
        username)
    }
  }

  def userIdLink(
    userId: String,
    cssClass: Option[String]): Html = userIdLink(userId.some, cssClass)

  def userIdLinkMini(userId: String) = Html {
    """<a href="%s">%s</a>""".format(
      routes.User show userId,
      (cached username userId) | userId
    )
  }

  def userLink(
    user: User,
    cssClass: Option[String] = None,
    withElo: Boolean = true,
    withOnline: Boolean = true,
    text: Option[String] = None) = Html {
    """<a class="user_link%s%s" href="%s">%s</a>""".format(
      withOnline.fold(
        isUsernameOnline(user.id).fold(" online", " offline"),
        ""),
      ~cssClass.map(" " + _),
      routes.User.show(user.username),
      text | withElo.fold(user.usernameWithElo, user.username)
    )
  }

  def userInfosLink(
    username: String,
    elo: Option[Int],
    cssClass: Option[String] = None,
    withOnline: Boolean = true) = Html {
    """<a class="user_link%s%s" href="%s">%s</a>""".format(
      withOnline.fold(
        isUsernameOnline(username).fold(" online", " offline"),
        ""),
      ~cssClass.map(" " + _),
      routes.User.show(username),
      elo.fold(username)(e ⇒ "%s (%d)".format(username, e))
    )
  }
}
