package lila.app
package templating

import lila.user.{ User, Users }
import lila.user.Env.{ current ⇒ userEnv }

import controllers.routes

import play.api.templates.Html
import play.api.libs.concurrent.Execution.Implicits._

trait UserHelper {

  import userEnv._

  def userIdToUsername(userId: String): String =
    (usernameOrAnonymous(userId)).await

  def userIdToUsername(userId: Option[String]): String =
    userId.fold(Users.anonymous)(userIdToUsername)

  def isUsernameOnline(username: String) = usernameMemo get username

  def userIdLink(
    userId: Option[String],
    cssClass: Option[String] = None,
    withOnline: Boolean = true): Html = Html {
    (userId zmap usernameOption) map {
      _.fold(Users.anonymous) { username ⇒
        """<a class="user_link%s%s" href="%s">%s</a>""".format(
          withOnline ??  isUsernameOnline(username).fold(" online", " offline"),
          cssClass.zmap(" " + _),
          routes.User.show(username),
          username)
      }
    } await
  }

  def userIdLink(
    userId: String,
    cssClass: Option[String]): Html = userIdLink(userId.some, cssClass)

  def userIdLinkMini(userId: String) = Html {
    usernameOption(userId) map { username ⇒
      """<a href="%s">%s</a>""".format(
        routes.User show userId,
        username | userId
      )
    } await
  }

  def userLink(
    user: User,
    cssClass: Option[String] = None,
    withElo: Boolean = true,
    withOnline: Boolean = true,
    text: Option[String] = None) = Html {
    """<a class="user_link%s%s" href="%s">%s</a>""".format(
      withOnline ??  isUsernameOnline(user.id).fold(" online", " offline"),
      cssClass.zmap(" " + _),
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
      withOnline ??  isUsernameOnline(username).fold(" online", " offline"),
      cssClass.zmap(" " + _),
      routes.User.show(username),
      elo.fold(username)(e ⇒ "%s (%d)".format(username, e))
    )
  }
}
