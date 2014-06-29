package lila.app
package templating

import controllers.routes
import mashup._
import play.twirl.api.Html

import lila.common.LightUser
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

  def lightUser(userId: String): Option[LightUser] = Env.user lightUser userId
  def lightUser(userId: Option[String]): Option[LightUser] = userId flatMap lightUser

  def usernameOrId(userId: String) = lightUser(userId).fold(userId)(_.titleName)
  def usernameOrAnon(userId: Option[String]) = lightUser(userId).fold(User.anonymous)(_.titleName)

  def isOnline(userId: String) = Env.user isOnline userId

  def userIdLink(
    userIdOption: Option[String],
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withTitle: Boolean = true,
    truncate: Option[Int] = None,
    params: String = ""): Html = Html {
    userIdOption.flatMap(lightUser).fold(User.anonymous) { user =>
      userIdNameLink(
        userId = user.id,
        username = user.name,
        title = user.title,
        cssClass = cssClass,
        withOnline = withOnline,
        withTitle = withTitle,
        truncate = truncate,
        params = params)
    }
  }

  def lightUserLink(
    user: LightUser,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withTitle: Boolean = true,
    truncate: Option[Int] = None,
    params: String = ""): Html = Html {
    userIdNameLink(
      userId = user.id,
      username = user.name,
      title = user.title,
      cssClass = cssClass,
      withOnline = withOnline,
      withTitle = withTitle,
      truncate = truncate,
      params = params)
  }

  def userIdLink(
    userId: String,
    cssClass: Option[String]): Html = userIdLink(userId.some, cssClass)

  def userIdLinkMini(userId: String) = Html {
    val user = lightUser(userId)
    val name = user.fold(userId)(_.name)
    val content = user.fold(userId)(_.titleNameHtml)
    val klass = userClass(userId, none, false)
    val href = userHref(name)
    s"""<a data-icon="r" $klass $href>&nbsp;$content</a>"""
  }

  def usernameLink(
    usernameOption: Option[String],
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withTitle: Boolean = true,
    truncate: Option[Int] = None): Html = Html {
    usernameOption.fold(User.anonymous) { username =>
      userIdNameLink(username.toLowerCase, username, cssClass, withOnline, withTitle, truncate)
    }
  }

  private def titleTag(title: Option[String]) = title match {
    case None    => ""
    case Some(t) => s"""<span class="title" title="${User titleName t}">$t</span>&nbsp;"""
  }

  private def userIdNameLink(
    userId: String,
    username: String,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withTitle: Boolean = true,
    truncate: Option[Int] = None,
    title: Option[String] = None,
    params: String = ""): String = {
    val klass = userClass(userId, cssClass, withOnline)
    val href = userHref(username, params = params)
    val content = truncate.fold(username)(username.take)
    val titleS = if (withTitle) titleTag(title) else ""
    val space = if (withOnline) "&nbsp;" else ""
    val dataIcon = if (withOnline) """ data-icon="r"""" else ""
    s"""<a$dataIcon $klass $href>$space$titleS$content</a>"""
  }

  def userLink(
    user: User,
    cssClass: Option[String] = None,
    withRating: Boolean = true,
    withProgress: Boolean = false,
    withOnline: Boolean = true,
    withPowerTip: Boolean = true,
    withTitle: Boolean = true,
    text: Option[String] = None,
    mod: Boolean = false) = Html {
    val klass = userClass(user.id, cssClass, withOnline, withPowerTip)
    val href = userHref(user.username, if (mod) "?mod" else "")
    val content = text | withRating.fold(user.usernameWithRating, user.username)
    val titleS = if (withTitle) titleTag(user.title) else ""
    val progress = if (withProgress) " " + showProgress(user.progress) else ""
    val space = if (withOnline) "&nbsp;" else ""
    val dataIcon = if (withOnline) """ data-icon="r"""" else ""
    s"""<a$dataIcon $klass $href>$space$titleS$content$progress</a>"""
  }

  def userInfosLink(
    userId: String,
    rating: Option[Int],
    cssClass: Option[String] = None,
    title: Option[String] = None,
    withOnline: Boolean = true) = {
    val user = lightUser(userId)
    val name = user.fold(userId)(_.name)
    val klass = userClass(userId, cssClass, withOnline)
    val href = userHref(name)
    val content = rating.fold(name)(e => s"$name&nbsp;($e)")
    val titleS = titleTag(title)
    val space = if (withOnline) "&nbsp;" else ""
    val dataIcon = if (withOnline) """ data-icon="r"""" else ""
    Html(s"""<a$dataIcon $klass $href>$space$titleS$content</a>""")
  }

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

  def describeUser(user: User) = {
    val name = user.titleUsername
    val nbGames = user.count.game
    val createdAt = org.joda.time.format.DateTimeFormat forStyle "M-" print user.createdAt
    val rating = user.perfs.global.intRating
    s"$name played $nbGames games since $createdAt. Current rating: $rating."
  }
}
