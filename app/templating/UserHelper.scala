package lila.app
package templating

import controllers.routes
import mashup._
import play.twirl.api.Html

import lila.api.Context
import lila.common.LightUser
import lila.rating.{ PerfType, Perf }
import lila.user.{ User, UserContext, Perfs }

trait UserHelper { self: I18nHelper with StringHelper with NumberHelper =>

  def showProgress(progress: Int, withTitle: Boolean = true) = Html {
    val span = progress match {
      case 0          => ""
      case p if p > 0 => s"""<span class="positive" data-icon="N">$p</span>"""
      case p if p < 0 => s"""<span class="negative" data-icon="M">${math.abs(p)}</span>"""
    }
    val title = if (withTitle) """data-hint="Rating progression over the last twelve games"""" else ""
    val klass = if (withTitle) "progress hint--bottom" else "progress"
    s"""<span $title class="$klass">$span</span>"""
  }

  val topBarSortedPerfTypes: List[PerfType] = List(
    PerfType.Bullet,
    PerfType.Chess960,
    PerfType.Blitz,
    PerfType.KingOfTheHill,
    PerfType.Classical,
    PerfType.ThreeCheck,
    PerfType.Correspondence,
    PerfType.Antichess,
    PerfType.Atomic,
    PerfType.Horde,
    PerfType.RacingKings,
    PerfType.Crazyhouse)

  def showPerfRating(rating: Int, name: String, nb: Int, provisional: Boolean, icon: Char, klass: String)(implicit ctx: Context) = Html {
    val title = s"$name rating over ${nb.localize} games"
    val attr = if (klass == "title") "title" else "data-hint"
    val number = if (nb > 0) s"$rating${if (provisional) "?" else ""}"
    else "&nbsp;&nbsp;&nbsp;-"
    s"""<span $attr="$title" class="$klass"><span data-icon="$icon">$number</span></span>"""
  }

  def showPerfRating(perfType: PerfType, perf: Perf, klass: String)(implicit ctx: Context): Html =
    showPerfRating(perf.intRating, perfType.name, perf.nb, perf.provisional, perfType.iconChar, klass)

  def showPerfRating(u: User, perfType: PerfType, klass: String = "hint--bottom")(implicit ctx: Context): Html =
    showPerfRating(perfType, u perfs perfType, klass)

  def showPerfRating(u: User, perfKey: String)(implicit ctx: Context): Option[Html] =
    PerfType(perfKey) map { showPerfRating(u, _) }

  def showBestPerf(u: User)(implicit ctx: Context): Option[Html] = u.perfs.bestPerf map {
    case (pt, perf) => showPerfRating(pt, perf, klass = "hint--bottom")
  }

  def showRatingDiff(diff: Int) = Html {
    diff match {
      case 0          => """<span class="rp null">±0</span>"""
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
    withOnline: Boolean = true,
    withPowerTip: Boolean = true,
    withDonor: Boolean = false,
    withTitle: Boolean = true,
    withBestRating: Boolean = false,
    withPerfRating: Option[PerfType] = None,
    text: Option[String] = None,
    params: String = "") = Html {
    val klass = userClass(user.id, cssClass, withOnline, withPowerTip)
    val href = userHref(user.username, params)
    val content = text | user.username
    val titleS = if (withTitle) titleTag(user.title) else ""
    val space = if (withOnline) "&nbsp;" else ""
    val dataIcon = if (withOnline) """ data-icon="r"""" else ""
    val rating = userRating(user, withPerfRating, withBestRating)
    val donor = if (withDonor) donorBadge else ""
      s"""<a$dataIcon $klass $href>$space$titleS$content$rating$donor</a>"""
  }

  def userInfosLink(
    userId: String,
    rating: Option[Int],
    cssClass: Option[String] = None,
    withPowerTip: Boolean = true,
    withTitle: Boolean = false,
    withOnline: Boolean = true) = {
    val user = lightUser(userId)
    val name = user.fold(userId)(_.name)
    val klass = userClass(userId, cssClass, withOnline, withPowerTip)
    val href = userHref(name)
    val content = rating.fold(name)(e => s"$name&nbsp;($e)")
    val titleS = titleTag(user.flatMap(_.title) ifTrue withTitle)
    val space = if (withOnline) "&nbsp;" else ""
    val dataIcon = if (withOnline) """ data-icon="r"""" else ""
    Html(s"""<a$dataIcon $klass $href>$space$titleS$content</a>""")
  }

  def userSpan(
    user: User,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withPowerTip: Boolean = true,
    withTitle: Boolean = true,
    withBestRating: Boolean = false,
    withPerfRating: Option[PerfType] = None,
    text: Option[String] = None) = Html {
    val klass = userClass(user.id, cssClass, withOnline, withPowerTip)
    val href = s"data-${userHref(user.username)}"
    val content = text | user.username
    val titleS = if (withTitle) titleTag(user.title) else ""
    val space = if (withOnline) "&nbsp;" else ""
    val dataIcon = if (withOnline) """ data-icon="r"""" else ""
    val rating = userRating(user, withPerfRating, withBestRating)
    s"""<span$dataIcon $klass $href>$space$titleS$content$rating</span>"""
  }

  def userIdSpanMini(userId: String, withOnline: Boolean = false) = Html {
    val user = lightUser(userId)
    val name = user.fold(userId)(_.name)
    val content = user.fold(userId)(_.titleNameHtml)
    val klass = userClass(userId, none, false)
    val href = s"data-${userHref(name)}"
    val space = if (withOnline) "&nbsp;" else ""
    val dataIcon = if (withOnline) """ data-icon="r"""" else ""
    s"""<span$dataIcon $klass $href>$space$content</span>"""
  }

  private def renderRating(perf: Perf) =
    s"&nbsp;(${perf.intRating}${if (perf.provisional) "?" else ""})"

  private def userRating(user: User, withPerfRating: Option[PerfType], withBestRating: Boolean) =
    withPerfRating match {
      case Some(perfType) => renderRating(user.perfs(perfType))
      case _ if withBestRating => user.perfs.bestPerf ?? {
        case (_, perf) => renderRating(perf)
      }
      case _ => ""
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
    case GameFilter.Imported => trans.nbImportedGames(info.nbImported)
    case GameFilter.Search   => Html(trans.advancedSearch.str().replaceFirst(" ", "\n"))
  }).toString)

  def describeUser(user: User) = {
    val name = user.titleUsername
    val nbGames = user.count.game
    val createdAt = org.joda.time.format.DateTimeFormat forStyle "M-" print user.createdAt
    val currentRating = user.perfs.bestPerf ?? {
      case (pt, perf) => s" Current ${pt.name} rating: ${perf.intRating}."
    }
    s"$name played $nbGames games since $createdAt.$currentRating"
  }

  def jsUserId(implicit ctx: Context) = Html {
    ctx.userId.fold("null")(id => s""""$id"""")
  }

  private val donorBadge = """<span data-icon="&#xe001;" class="donor is-gold" title="Lichess donor"></span>"""
}
