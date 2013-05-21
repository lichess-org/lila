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
    truncate: Option[Int] = None,
    go: Boolean = false): Html = Html {
    userIdOption.fold(User.anonymous) { userId ⇒
      Env.user usernameOption userId map {
        _.fold(User.anonymous) { username ⇒
          userIdNameLink(userId, username, cssClass, withOnline, truncate, go)
        }
      } await
    }
  }

  def userIdLink(
    userId: String,
    cssClass: Option[String]): Html = userIdLink(userId.some, cssClass)

  def userIdLinkMini(userId: String) = Html {
    Env.user usernameOption userId map { username ⇒
      """<a %s %s>%s</a>""".format(
        userClass(userId, none, false, false),
        userHref(username | userId),
        username | userId
      )
    } await
  }

  def usernameLink(
    usernameOption: Option[String],
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    truncate: Option[Int] = None,
    go: Boolean = false): Html = Html {
    usernameOption.fold(User.anonymous) { username ⇒
      userIdNameLink(username.toLowerCase, username, cssClass, withOnline, truncate, go)
    }
  }

  private def userIdNameLink(
    userId: String,
    username: String,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    truncate: Option[Int] = None,
    go: Boolean = false): String =
    """<a %s %s>%s</a>""".format(
      userClass(userId, cssClass, withOnline, go),
      userHref(username),
      truncate.fold(username)(username.take)
    )

  def userLink(
    user: User,
    cssClass: Option[String] = None,
    withElo: Boolean = true,
    withOnline: Boolean = true,
    text: Option[String] = None,
    go: Boolean = false) = Html {
    """<a %s %s>%s</a>""".format(
      userClass(user.id, cssClass, withOnline, go),
      userHref(user.username),
      text | withElo.fold(user.usernameWithElo, user.username)
    )
  }

  def userInfosLink(
    userId: String,
    elo: Option[Int],
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    go: Boolean = false) = Env.user usernameOption userId map (_ | userId) map { username ⇒
    Html {
      """<a %s %s>%s</a>""".format(
        userClass(userId, cssClass, withOnline, go),
        userHref(username),
        elo.fold(username)(e ⇒ "%s (%d)".format(username, e))
      )
    }
  } await

  private def userHref(username: String) =
    "href=\"" + routes.User.show(username) + "\""

  private def userClass(
    userId: String,
    cssClass: Option[String],
    go: Boolean,
    withOnline: Boolean) = {
    "user_link" :: List(
      cssClass,
      go option "go",
      withOnline option isOnline(userId).fold("online", "offline")
    ).flatten
  }.mkString("class=\"", " ", "\"")
}
