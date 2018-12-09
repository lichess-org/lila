package lila.app
package templating

import play.twirl.api.Html

import controllers.routes
import mashup._

import lila.api.Context
import lila.common.LightUser
import lila.i18n.I18nKeys
import lila.rating.{ PerfType, Perf }
import lila.user.{ User, Title, UserContext }

trait UserHelper { self: I18nHelper with StringHelper with HtmlHelper with NumberHelper =>

  def showProgress(progress: Int, withTitle: Boolean = true) = Html {
    val span = progress match {
      case 0 => ""
      case p if p > 0 => s"""<span class="positive" data-icon="N">$p</span>"""
      case p if p < 0 => s"""<span class="negative" data-icon="M">${math.abs(p)}</span>"""
    }
    val title = if (withTitle) """ data-hint="Rating progression over the last twelve games"""" else ""
    val klass = if (withTitle) "progress hint--bottom" else "progress"
    s"""<span$title class="$klass">$span</span>"""
  }

  val topBarSortedPerfTypes: List[PerfType] = List(
    PerfType.Bullet,
    PerfType.Chess960,
    PerfType.Blitz,
    PerfType.KingOfTheHill,
    PerfType.Rapid,
    PerfType.ThreeCheck,
    PerfType.Classical,
    PerfType.Antichess,
    PerfType.Correspondence,
    PerfType.Atomic,
    PerfType.Horde,
    PerfType.Crazyhouse
  )

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
  def showBestPerfs(u: User, nb: Int)(implicit ctx: Context): Html = Html {
    u.perfs.bestPerfs(nb) map {
      case (pt, perf) => showPerfRating(pt, perf, klass = "hint--bottom").body
    } mkString " "
  }

  def showRatingDiff(diff: Int) = Html {
    diff match {
      case 0 => """<span class="rp null">±0</span>"""
      case d if d > 0 => s"""<span class="rp up">+$d</span>"""
      case d => s"""<span class="rp down">−${-d}</span>"""
    }
  }

  def lightUser(userId: String): Option[LightUser] = Env.user lightUserSync userId
  def lightUser(userId: Option[String]): Option[LightUser] = userId flatMap lightUser

  // def lightUserSync: LightUser.SyncGetter(userId: String): Option[LightUser] = Env.user lightUserSync userId

  def usernameOrId(userId: String) = lightUser(userId).fold(userId)(_.titleName)
  def usernameOrAnon(userId: Option[String]) = lightUser(userId).fold(User.anonymous)(_.titleName)

  def isOnline(userId: String) = Env.user isOnline userId

  def isStreaming(userId: String) = Env.streamer.liveStreamApi isStreaming userId

  def userIdLink(
    userIdOption: Option[String],
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withTitle: Boolean = true,
    truncate: Option[Int] = None,
    params: String = "",
    modIcon: Boolean = false
  ): Html = Html {
    userIdOption.flatMap(lightUser).fold(User.anonymous) { user =>
      userIdNameLink(
        userId = user.id,
        username = user.name,
        isPatron = user.isPatron,
        title = withTitle ?? user.title map Title.apply,
        cssClass = cssClass,
        withOnline = withOnline,
        truncate = truncate,
        params = params,
        modIcon = modIcon
      )
    }
  }

  def lightUserLink(
    user: LightUser,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withTitle: Boolean = true,
    truncate: Option[Int] = None,
    params: String = ""
  ): Html = Html {
    userIdNameLink(
      userId = user.id,
      username = user.name,
      isPatron = user.isPatron,
      title = withTitle ?? user.title map Title.apply,
      cssClass = cssClass,
      withOnline = withOnline,
      truncate = truncate,
      params = params,
      modIcon = false
    )
  }

  def userIdLink(
    userId: String,
    cssClass: Option[String]
  ): Html = userIdLink(userId.some, cssClass)

  def titleTag(title: Option[Title]) = Html {
    title.fold("") { t =>
      s"""<span class="title"${(t == Title.BOT) ?? " data-bot"} title="${Title titleName t}">$t</span>&nbsp;"""
    }
  }
  def titleTag(lu: LightUser): Html = titleTag(lu.title map Title.apply)

  private def userIdNameLink(
    userId: String,
    username: String,
    isPatron: Boolean,
    cssClass: Option[String],
    withOnline: Boolean,
    truncate: Option[Int],
    title: Option[Title],
    params: String,
    modIcon: Boolean
  ): String = {
    val klass = userClass(userId, cssClass, withOnline)
    val href = userHref(username, params = params)
    val content = truncate.fold(username)(username.take)
    val titleS = titleTag(title).body
    val icon = withOnline ?? (if (modIcon) moderatorIcon else lineIcon(isPatron))
    s"""<a $klass $href>$icon$titleS$content</a>"""
  }

  def userLink(
    user: User,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withPowerTip: Boolean = true,
    withTitle: Boolean = true,
    withBestRating: Boolean = false,
    withPerfRating: Option[PerfType] = None,
    text: Option[String] = None,
    params: String = ""
  ): Html = Html {
    val klass = userClass(user.id, cssClass, withOnline, withPowerTip)
    val href = userHref(user.username, params)
    val content = text | user.username
    val titleS = if (withTitle) titleTag(user.title).body else ""
    val rating = userRating(user, withPerfRating, withBestRating)
    val icon = withOnline ?? lineIcon(user)
    s"""<a $klass $href>$icon$titleS$content$rating</a>"""
  }

  def userInfosLink(
    userId: String,
    rating: Option[Int],
    cssClass: Option[String] = None,
    withPowerTip: Boolean = true,
    withTitle: Boolean = false,
    withOnline: Boolean = true
  ) = {
    val user = lightUser(userId)
    val name = user.fold(userId)(_.name)
    val klass = userClass(userId, cssClass, withOnline, withPowerTip)
    val href = userHref(name)
    val rat = rating ?? { r => s" ($r)" }
    val titleS = withTitle ?? user ?? (u => titleTag(u).body)
    val icon = withOnline ?? lineIcon(user)
    Html(s"""<a $klass $href>$icon$titleS$name$rat</a>""")
  }

  def userSpan(
    user: User,
    cssClass: Option[String] = None,
    withOnline: Boolean = true,
    withPowerTip: Boolean = true,
    withTitle: Boolean = true,
    withBestRating: Boolean = false,
    withPerfRating: Option[PerfType] = None,
    text: Option[String] = None
  ) = Html {
    val klass = userClass(user.id, cssClass, withOnline, withPowerTip)
    val href = s"data-${userHref(user.username)}"
    val content = text | user.username
    val titleS = if (withTitle) titleTag(user.title).body else ""
    val rating = userRating(user, withPerfRating, withBestRating)
    val icon = withOnline ?? lineIcon(user)
    s"""<span $klass $href>$icon$titleS$content$rating</span>"""
  }

  def userIdSpanMini(userId: String, withOnline: Boolean = false) = Html {
    val user = lightUser(userId)
    val name = user.fold(userId)(_.name)
    val content = user.fold(userId)(_.name)
    val titleS = user.??(u => titleTag(u.title map Title.apply).body)
    val klass = userClass(userId, none, withOnline)
    val href = s"data-${userHref(name)}"
    val icon = withOnline ?? lineIcon(user)
    s"""<span $klass $href>$icon$titleS$content</span>"""
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

  private def addClass(cls: Option[String]) = cls.fold("")(" " + _)

  protected def userClass(
    userId: String,
    cssClass: Option[String],
    withOnline: Boolean,
    withPowerTip: Boolean = true
  ): String = {
    val online = if (withOnline) {
      if (isOnline(userId)) " online" else " offline"
    } else ""
    s"""class="user_link${addClass(cssClass)}${addClass(withPowerTip option "ulpt")}$online""""
  }

  def userGameFilterTitle(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(implicit ctx: UserContext) =
    splitNumber(userGameFilterTitleNoTag(u, nbs, filter))

  def userGameFilterTitleNoTag(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(implicit ctx: UserContext): Html = (filter match {
    case GameFilter.All => I18nKeys.nbGames.pluralSame(u.count.game)
    case GameFilter.Me => nbs.withMe ?? I18nKeys.nbGamesWithYou.pluralSame
    case GameFilter.Rated => I18nKeys.nbRated.pluralSame(u.count.rated)
    case GameFilter.Win => I18nKeys.nbWins.pluralSame(u.count.win)
    case GameFilter.Loss => I18nKeys.nbLosses.pluralSame(u.count.loss)
    case GameFilter.Draw => I18nKeys.nbDraws.pluralSame(u.count.draw)
    case GameFilter.Playing => I18nKeys.nbPlaying.pluralSame(nbs.playing)
    case GameFilter.Bookmark => I18nKeys.nbBookmarks.pluralSame(nbs.bookmark)
    case GameFilter.Imported => I18nKeys.nbImportedGames.pluralSame(nbs.imported)
    case GameFilter.Search => I18nKeys.advancedSearch()
  })

  def describeUser(user: User) = {
    val name = user.titleUsername
    val nbGames = user.count.game
    val createdAt = org.joda.time.format.DateTimeFormat forStyle "M-" print user.createdAt
    val currentRating = user.perfs.bestPerf ?? {
      case (pt, perf) => s" Current ${pt.name} rating: ${perf.intRating}."
    }
    s"$name played $nbGames games since $createdAt.$currentRating"
  }

  val patronIconChar = ""
  val lineIconChar = ""

  val lineIcon: String = """<i class="line"></i>"""
  val patronIcon: String = """<i class="line patron" title="lichess Patron"></i>"""
  val moderatorIcon: String = """<i class="line moderator" title="lichess Moderator"></i>"""
  private def lineIcon(patron: Boolean): String = if (patron) patronIcon else lineIcon
  private def lineIcon(user: Option[LightUser]): String = lineIcon(user.??(_.isPatron))
  def lineIcon(user: LightUser): String = lineIcon(user.isPatron)
  def lineIcon(user: User): String = lineIcon(user.isPatron)
  def lineIconChar(user: User): String = if (user.isPatron) patronIconChar else lineIconChar
}
