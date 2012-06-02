package lila
package user

import core.CoreEnv
import controllers.routes

import play.api.templates.Html

trait UserHelper {

  protected def env: CoreEnv

  private def cached = env.user.cached
  private def usernameMemo = env.user.usernameMemo

  def userIdToUsername(userId: String): String =
    cached usernameOrAnonymous userId

  def userIdToUsername(userId: Option[String]): String =
    userId.fold(userIdToUsername, User.anonymous)

  def isUsernameOnline(username: String) = usernameMemo get username

  def userIdLink(
    userId: Option[String],
    cssClass: Option[String] = None,
    withOnline: Boolean = true): Html = Html {
    (userId flatMap cached.username).fold(
      username ⇒ """<a class="user_link%s%s" href="%s">%s</a>""".format(
        withOnline.fold(
          isUsernameOnline(username).fold(" online", " offline"),
          ""),
        cssClass.fold(" " + _, ""),
        routes.User.show(username),
        username),
      User.anonymous
    )
  }

  def userIdLink(
    userId: String,
    cssClass: Option[String]): Html = userIdLink(userId.some, cssClass)

  def userLink(
    user: User,
    cssClass: Option[String] = None,
    withElo: Boolean = true) = Html {
    """<a class="user_link%s%s" href="%s">%s</a>""".format(
      isUsernameOnline(user.username).fold(" online", " offline"),
      cssClass.fold(" " + _, ""),
      routes.User.show(user.username),
      withElo.fold(user.usernameWithElo, user.username))
  }
}
