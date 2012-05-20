package lila
package user

import core.CoreEnv
import controllers.routes

import play.api.templates.Html

trait UserHelper {

  protected def env: CoreEnv

  private def cached = env.user.cached
  private def usernameMemo = env.user.usernameMemo

  def userIdToUsername(userId: String) = cached username userId

  def isUsernameOnline(username: String) = usernameMemo get username

  def isUserIdOnline(userId: String) =
    usernameMemo get userIdToUsername(userId)

  def userLink(
    user: User,
    cssClass: Option[String] = None,
    withElo: Boolean = false) = Html {
    """<a class="user_link%s%s" href="%s">%s</a>""".format(
      isUsernameOnline(user.username).fold(" online", ""),
      cssClass.fold(" " + _, ""),
      routes.User.show(user.username),
      withElo.fold(user.usernameWithElo, user.username))
  }
}
