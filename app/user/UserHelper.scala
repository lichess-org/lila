package lila
package user

import core.CoreEnv
import controllers.routes

import com.mongodb.casbah.Imports.ObjectId
import play.api.templates.Html

trait UserHelper {

  protected def env: CoreEnv

  private def cached = env.user.cached
  private def usernameMemo = env.user.usernameMemo

  def userIdToUsername(userId: String): String = cached username userId

  def userIdToUsername(userId: Option[String]): String =
    userId.fold(userIdToUsername, User.anonymous)

  def isUsernameOnline(username: String) = usernameMemo get username

  def isUserIdOnline(userId: String) =
    usernameMemo get userIdToUsername(userId)

  def userIdLink(
    userId: Option[String],
    cssClass: Option[String]): Html = Html {
    (userId map userIdToUsername).fold(
      username ⇒ """<a class="user_link%s%s" href="%s">%s</a>""".format(
        isUsernameOnline(username).fold(" online", ""),
        cssClass.fold(" " + _, ""),
        routes.User.show(username),
        username),
      User.anonymous
    )
  }

  def userIdLink(
    userId: ObjectId,
    cssClass: Option[String]): Html = userIdLink(userId.toString.some, cssClass)

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
