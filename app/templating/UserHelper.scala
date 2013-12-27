package lila.app
package templating

import controllers.routes
import mashup._
import play.api.templates.Html

import lila.user.{ User, UserContext }

trait UserHelper { self: I18nHelper with StringHelper ⇒

  def showProgress(progress: Int) = Html {
    val title = "Rating progression over the last ten games"
    val span = progress match {
      case 0          ⇒ s"""<span class="zero">=</span>"""
      case p if p > 0 ⇒ s"""<span class="positive">$p↗</span>"""
      case p if p < 0 ⇒ s"""<span class="negative">${math.abs(p)}↘</span>"""
    }
    s"""<span title="$title" class="progress">$span</span>"""
  }

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
      """<a %s %s>%s</a>""".format(
        userClass(userId, none, false),
        userHref(username | userId),
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
    """<a %s %s>%s</a>""".format(
      userClass(userId, cssClass, withOnline),
      userHref(username),
      truncate.fold(username)(username.take)
    )

  def userLink(
    user: User,
    cssClass: Option[String] = None,
    withRating: Boolean = true,
    withProgress: Boolean = false,
    withOnline: Boolean = true,
    text: Option[String] = None) = Html {
    val klass = userClass(user.id, cssClass, withOnline)
    val href = userHref(user.username)
    val content = text | withRating.fold(user.usernameWithRating, user.username)
    val progress = withProgress ?? (" " + showProgress(user.progress))
    s"""<a $klass $href>$content$progress</a>"""
  }

  def userInfosLink(
    userId: String,
    rating: Option[Int],
    cssClass: Option[String] = None,
    withOnline: Boolean = true) = Env.user usernameOption userId map (_ | userId) map { username ⇒
    Html {
      """<a %s %s>%s</a>""".format(
        userClass(userId, cssClass, withOnline),
        userHref(username),
        rating.fold(username)(e ⇒ s"$username ($e)")
      )
    }
  } await

  def perfTitle(perf: String): String = lila.user.Perf.titles get perf getOrElse perf

  private def userHref(username: String) =
    "href=\"" + routes.User.show(username) + "\""

  protected def userClass(
    userId: String,
    cssClass: Option[String],
    withOnline: Boolean) = {
    // ultp = user link power tip
    "user_link" :: "ulpt" :: List(
      cssClass,
      withOnline option isOnline(userId).fold("online", "offline")
    ).flatten
  }.mkString("class=\"", " ", "\"")

  def userGameFilterTitle(info: UserInfo, filter: GameFilter)(implicit ctx: UserContext) =
    splitNumber(userGameFilterTitleNoTag(info, filter))

  def userGameFilterTitleNoTag(info: UserInfo, filter: GameFilter)(implicit ctx: UserContext) = Html((filter match {
    case GameFilter.All      ⇒ info.user.count.game + " " + trans.gamesPlayed()
    case GameFilter.Me       ⇒ ctx.me ?? (me ⇒ trans.nbGamesWithYou.str(info.nbWithMe))
    case GameFilter.Rated    ⇒ info.nbRated + " " + trans.rated()
    case GameFilter.Win      ⇒ trans.nbWins(info.user.count.win)
    case GameFilter.Loss     ⇒ trans.nbLosses(info.user.count.loss)
    case GameFilter.Draw     ⇒ trans.nbDraws(info.user.count.draw)
    case GameFilter.Playing  ⇒ info.nbPlaying + " playing"
    case GameFilter.Bookmark ⇒ trans.nbBookmarks(info.nbBookmark)
  }).toString)
}
