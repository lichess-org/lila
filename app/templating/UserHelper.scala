package lila.app
package templating

import controllers.routes
import mashup._
import play.api.templates.Html

import lila.user.{ User, UserContext }

trait UserHelper { self: I18nHelper with StringHelper =>

  def showProgress(progress: Int) = Html {
    val title = "Rating progression over the last ten games"
    val span = progress match {
      case 0          => ""
      case p if p > 0 => s"""<span class="positive" data-icon="N">$p</span>"""
      case p if p < 0 => s"""<span class="negative" data-icon="M">${math.abs(p)}</span>"""
    }
    s"""<span title="$title" class="progress">$span</span>"""
  }

  def showRatingDiff(diff: Int) = Html {
    diff match {
      case 0          => """<span class="rp null">+0</span>"""
      case d if d > 0 => s"""<span class="rp up">+$d</span>"""
      case d          => s"""<span class="rp down">$d</span>"""
    }
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
    truncate: Option[Int] = None,
    params: String = ""): Html = Html {
    userIdOption.fold(User.anonymous) { userId =>
      Env.user usernameOption userId map {
        _.fold(User.anonymous) { username =>
          userIdNameLink(
            userId = userId,
            username = username,
            cssClass = cssClass,
            withOnline = withOnline,
            truncate = truncate,
            params = params)
        }
      } await
    }
  }

  def userIdLink(
    userId: String,
    cssClass: Option[String]): Html = userIdLink(userId.some, cssClass)

  def userIdLinkMini(userId: String) = Html {
    Env.user usernameOption userId map { username =>
      val klass = userClass(userId, none, false)
      val href = userHref(username getOrElse userId)
      val content = username getOrElse userId
      s"""<a data-icon="r" $klass $href>&nbsp;$content</a>"""
    } await
  }

  def usernameLink(
    usernameOption: Option[String],
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    truncate: Option[Int] = None): Html = Html {
    usernameOption.fold(User.anonymous) { username =>
      userIdNameLink(username.toLowerCase, username, cssClass, withOnline, truncate)
    }
  }

  private def userIdNameLink(
    userId: String,
    username: String,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    truncate: Option[Int] = None,
    params: String = ""): String = {
    val klass = userClass(userId, cssClass, withOnline)
    val href = userHref(username, params = params)
    val content = truncate.fold(username)(username.take)
    val space = if (withOnline) "&nbsp;" else ""
    val dataIcon = if (withOnline) """data-icon="r"""" else ""
    s"""<a $dataIcon $klass $href>$space$content</a>"""
  }

  def userLink(
    user: User,
    cssClass: Option[String] = None,
    withRating: Boolean = true,
    withProgress: Boolean = false,
    withOnline: Boolean = true,
    withPowerTip: Boolean = true,
    text: Option[String] = None,
    mod: Boolean = false) = Html {
    val klass = userClass(user.id, cssClass, withOnline, withPowerTip)
    val href = userHref(user.username, if (mod) "?mod" else "")
    val content = text | withRating.fold(user.usernameWithRating, user.username)
    val progress = if(withProgress) " " + showProgress(user.progress) else ""
    val space = if (withOnline) "&nbsp;" else ""
    val dataIcon = if (withOnline) """data-icon="r"""" else ""
    s"""<a $dataIcon $klass $href>$space$content$progress</a>"""
  }

  def userInfosLink(
    userId: String,
    rating: Option[Int],
    cssClass: Option[String] = None,
    withOnline: Boolean = true) = Env.user usernameOption userId map (_ | userId) map { username =>
    Html {
      val klass = userClass(userId, cssClass, withOnline)
      val href = userHref(username)
      val content = rating.fold(username)(e => s"$username ($e)")
      val space = if (withOnline) "&nbsp;" else ""
      val dataIcon = if (withOnline) """data-icon="r"""" else ""
      s"""<a $dataIcon $klass $href>$space$content</a>"""
    }
  } await

  def perfTitle(perf: String): String = lila.user.Perfs.titles get perf getOrElse perf

  private def userHref(username: String, params: String = "") =
    s"""href="${routes.User.show(username)}$params""""

  protected def userClass(
    userId: String,
    cssClass: Option[String],
    withOnline: Boolean,
    withPowerTip: Boolean = true) = {
    "user_link" :: List(
      cssClass,
      withPowerTip option "ulpt",
      withOnline option isOnline(userId).fold("online is-green", "offline")
    ).flatten
  }.mkString("class=\"", " ", "\"")

  def userGameFilterTitle(info: UserInfo, filter: GameFilter)(implicit ctx: UserContext) =
    splitNumber(userGameFilterTitleNoTag(info, filter))

  def userGameFilterTitleNoTag(info: UserInfo, filter: GameFilter)(implicit ctx: UserContext) = Html((filter match {
    case GameFilter.All      => info.user.count.game + " " + trans.gamesPlayed()
    case GameFilter.Me       => ctx.me ?? (me => trans.nbGamesWithYou.str(info.nbWithMe))
    case GameFilter.Rated    => info.nbRated + " " + trans.rated()
    case GameFilter.Win      => trans.nbWins(info.user.count.win)
    case GameFilter.Loss     => trans.nbLosses(info.user.count.loss)
    case GameFilter.Draw     => trans.nbDraws(info.user.count.draw)
    case GameFilter.Playing  => info.nbPlaying + " playing"
    case GameFilter.Bookmark => trans.nbBookmarks(info.nbBookmark)
  }).toString)
}
