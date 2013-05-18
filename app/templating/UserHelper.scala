package lila.app
package templating

import lila.user.User

import controllers.routes

import play.api.templates.Html

trait UserHelper {

  def userIdToUsername(userId: String): String =
    (Env.user usernameOrAnonymous userId).await

  def userIdToUsername(userId: Option[String]): String =
    userId.fold(User.anonymous)(userIdToUsername)

  def isOnline(userId: String) = Env.user isOnline userId

  def userIdLink(
    userIdOption: Option[String],
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    truncate: Option[Int] = None): Html = Html {
    userIdOption.fold(User.anonymous) { userId ⇒
      Env.user usernameOption userId map {
        _.fold(User.anonymous) { username ⇒
          userIdNameLink(userId, username, cssClass, withOnline, truncate)
        }
      } await
    }
  }

  def userIdLink(
    userId: String,
    cssClass: Option[String]): Html = userIdLink(userId.some, cssClass)

  def userIdLinkMini(userId: String) = Html {
    Env.user usernameOption userId map { username ⇒
      """<a href="%s">%s</a>""".format(
        routes.User show userId,
        username | userId
      )
    } await
  }

  def usernameLink(
    usernameOption: Option[String],
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    truncate: Option[Int] = None): Html = Html {
    usernameOption.fold(User.anonymous) { username ⇒
      userIdNameLink(username.toLowerCase, username, cssClass, withOnline, truncate)
    }
  }

  private def userIdNameLink(
    userId: String,
    username: String,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    truncate: Option[Int] = None): String =
    """<a class="user_link%s%s" href="%s">%s</a>""".format(
      withOnline ?? isOnline(userId).fold(" online", " offline"),
      cssClass.??(" " + _),
      routes.User.show(username),
      truncate.fold(username)(username.take)
    )

  def userLink(
    user: User,
    cssClass: Option[String] = None,
    withElo: Boolean = true,
    withOnline: Boolean = true,
    text: Option[String] = None) = Html {
    """<a class="user_link%s%s" href="%s">%s</a>""".format(
      withOnline ?? isOnline(user.id).fold(" online", " offline"),
      cssClass.??(" " + _),
      routes.User.show(user.username),
      text | withElo.fold(user.usernameWithElo, user.username)
    )
  }

  def userInfosLink(
    userId: String,
    elo: Option[Int],
    cssClass: Option[String] = None,
    withOnline: Boolean = true) = Env.user usernameOption userId map (_ | userId) map { username ⇒
    Html {
      """<a class="user_link%s%s" href="%s">%s</a>""".format(
        withOnline ?? isOnline(userId).fold(" online", " offline"),
        cssClass.??(" " + _),
        routes.User.show(username),
        elo.fold(username)(e ⇒ "%s (%d)".format(username, e))
      )
    }
  } await
}
