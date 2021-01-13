package lila.app
package templating

import controllers.routes
import mashup._
import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate._
import lila.common.LightUser
import lila.i18n.{ I18nKey, I18nKeys => trans }
import lila.rating.{ Perf, PerfType }
import lila.user.{ Title, User }

trait UserHelper { self: I18nHelper with StringHelper with NumberHelper =>

  def ratingProgress(progress: Int): Option[Frag] =
    if (progress > 0) goodTag(cls := "rp")(progress).some
    else if (progress < 0) badTag(cls := "rp")(math.abs(progress)).some
    else none

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

  def showPerfRating(rating: Int, name: String, nb: Int, provisional: Boolean, clueless: Boolean, icon: Char)(
      implicit lang: Lang
  ): Frag =
    span(
      title := s"$name rating over ${nb.localize} games",
      dataIcon := icon,
      cls := "text"
    )(
      if (clueless) frag(nbsp, nbsp, nbsp, if (nb < 1) "-" else "?")
      else frag(rating, provisional option "?")
    )

  def showPerfRating(perfType: PerfType, perf: Perf)(implicit lang: Lang): Frag =
    showPerfRating(
      perf.intRating,
      perfType.trans,
      perf.nb,
      perf.provisional,
      perf.clueless,
      perfType.iconChar
    )

  def showPerfRating(u: User, perfType: PerfType)(implicit lang: Lang): Frag =
    showPerfRating(perfType, u perfs perfType)

  def showPerfRating(u: User, perfKey: String)(implicit lang: Lang): Option[Frag] =
    PerfType(perfKey) map { showPerfRating(u, _) }

  def showBestPerf(u: User)(implicit lang: Lang): Option[Frag] =
    u.perfs.bestPerf map { case (pt, perf) =>
      showPerfRating(pt, perf)
    }
  def showBestPerfs(u: User, nb: Int)(implicit lang: Lang): List[Frag] =
    u.perfs.bestPerfs(nb) map { case (pt, perf) =>
      showPerfRating(pt, perf)
    }

  def showRatingDiff(diff: Int): Frag =
    diff match {
      case 0          => span("±0")
      case d if d > 0 => goodTag(s"+$d")
      case d          => badTag(s"−${-d}")
    }

  def lightUser = env.user.lightUserSync

  def usernameOrId(userId: String)           = lightUser(userId).fold(userId)(_.titleName)
  def usernameOrAnon(userId: Option[String]) = userId.flatMap(lightUser).fold(User.anonymous)(_.titleName)

  def isOnline(userId: String) = env.socket isOnline userId

  def isStreaming(userId: String) = env.streamer.liveStreamApi isStreaming userId

  def userIdLink(
      userIdOption: Option[String],
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withTitle: Boolean = true,
      truncate: Option[Int] = None,
      params: String = "",
      modIcon: Boolean = false
  )(implicit lang: Lang): Frag =
    userIdOption.flatMap(lightUser).fold[Frag](User.anonymous) { user =>
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

  def lightUserLink(
      user: LightUser,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withTitle: Boolean = true,
      truncate: Option[Int] = None,
      params: String = ""
  )(implicit lang: Lang): Frag =
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

  def userIdLink(
      userId: String,
      cssClass: Option[String]
  )(implicit lang: Lang): Frag = userIdLink(userId.some, cssClass)

  def titleTag(title: Option[Title]): Option[Frag] =
    title map { t =>
      frag(userTitleTag(t), nbsp)
    }
  def titleTag(lu: LightUser): Frag = titleTag(lu.title map Title.apply)

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
  )(implicit lang: Lang): Frag =
    a(
      cls := userClass(userId, cssClass, withOnline),
      href := userUrl(username, params = params)
    )(
      withOnline ?? (if (modIcon) moderatorIcon else lineIcon(isPatron)),
      titleTag(title),
      truncate.fold(username)(username.take)
    )

  def userLink(
      user: User,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withPowerTip: Boolean = true,
      withTitle: Boolean = true,
      withBestRating: Boolean = false,
      withPerfRating: Option[PerfType] = None,
      name: Option[Frag] = None,
      params: String = ""
  )(implicit lang: Lang): Frag =
    a(
      cls := userClass(user.id, cssClass, withOnline, withPowerTip),
      href := userUrl(user.username, params)
    )(
      withOnline ?? lineIcon(user),
      withTitle option titleTag(user.title),
      name | user.username,
      userRating(user, withPerfRating, withBestRating)
    )

  def userSpan(
      user: User,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withPowerTip: Boolean = true,
      withTitle: Boolean = true,
      withBestRating: Boolean = false,
      withPerfRating: Option[PerfType] = None,
      name: Option[Frag] = None
  )(implicit lang: Lang): Frag =
    span(
      cls := userClass(user.id, cssClass, withOnline, withPowerTip),
      dataHref := userUrl(user.username)
    )(
      withOnline ?? lineIcon(user),
      withTitle option titleTag(user.title),
      name | user.username,
      userRating(user, withPerfRating, withBestRating)
    )

  def userIdSpanMini(userId: String, withOnline: Boolean = false)(implicit lang: Lang): Frag = {
    val user = lightUser(userId)
    val name = user.fold(userId)(_.name)
    span(
      cls := userClass(userId, none, withOnline),
      dataHref := userUrl(name)
    )(
      withOnline ?? lineIcon(user),
      user.??(u => titleTag(u.title map Title.apply)),
      name
    )
  }

  private def renderRating(perf: Perf): Frag =
    frag(
      " (",
      perf.intRating,
      perf.provisional option "?",
      ")"
    )

  private def userRating(user: User, withPerfRating: Option[PerfType], withBestRating: Boolean): Frag =
    withPerfRating match {
      case Some(perfType) => renderRating(user.perfs(perfType))
      case _ if withBestRating =>
        user.perfs.bestPerf ?? { case (_, perf) =>
          renderRating(perf)
        }
      case _ => ""
    }

  private def userUrl(username: String, params: String = "") =
    s"""${routes.User.show(username)}$params"""

  protected def userClass(
      userId: String,
      cssClass: Option[String],
      withOnline: Boolean,
      withPowerTip: Boolean = true
  ): List[(String, Boolean)] =
    (withOnline ?? List((if (isOnline(userId)) "online" else "offline") -> true)) ::: List(
      "user-link" -> true,
      ~cssClass   -> cssClass.isDefined,
      "ulpt"      -> withPowerTip
    )

  def userGameFilterTitle(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(implicit
      lang: Lang
  ): Frag =
    if (filter == GameFilter.Search) frag(br, trans.search.advancedSearch())
    else splitNumber(userGameFilterTitleNoTag(u, nbs, filter))

  private def transLocalize(key: I18nKey, number: Int)(implicit lang: Lang) =
    key.pluralSameTxt(number)

  def userGameFilterTitleNoTag(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(implicit
      lang: Lang
  ): String =
    filter match {
      case GameFilter.All      => transLocalize(trans.nbGames, u.count.game)
      case GameFilter.Me       => nbs.withMe ?? { transLocalize(trans.nbGamesWithYou, _) }
      case GameFilter.Rated    => transLocalize(trans.nbRated, u.count.rated)
      case GameFilter.Win      => transLocalize(trans.nbWins, u.count.win)
      case GameFilter.Loss     => transLocalize(trans.nbLosses, u.count.loss)
      case GameFilter.Draw     => transLocalize(trans.nbDraws, u.count.draw)
      case GameFilter.Playing  => transLocalize(trans.nbPlaying, nbs.playing)
      case GameFilter.Bookmark => transLocalize(trans.nbBookmarks, nbs.bookmark)
      case GameFilter.Imported => transLocalize(trans.nbImportedGames, nbs.imported)
      case GameFilter.Search   => trans.search.advancedSearch.txt()
    }

  def describeUser(user: User)(implicit lang: Lang) = {
    val name      = user.titleUsername
    val nbGames   = user.count.game
    val createdAt = org.joda.time.format.DateTimeFormat forStyle "M-" print user.createdAt
    val currentRating = user.perfs.bestPerf ?? { case (pt, perf) =>
      s" Current ${pt.trans} rating: ${perf.intRating}."
    }
    s"$name played $nbGames games since $createdAt.$currentRating"
  }

  val patronIconChar = ""
  val lineIconChar   = ""

  val lineIcon: Frag = i(cls := "line")
  def patronIcon(implicit lang: Lang): Frag =
    i(cls := "line patron", title := trans.patron.lichessPatron.txt())
  val moderatorIcon: Frag                                                  = i(cls := "line moderator", title := "Lichess Mod")
  private def lineIcon(patron: Boolean)(implicit lang: Lang): Frag         = if (patron) patronIcon else lineIcon
  private def lineIcon(user: Option[LightUser])(implicit lang: Lang): Frag = lineIcon(user.??(_.isPatron))
  def lineIcon(user: LightUser)(implicit lang: Lang): Frag                 = lineIcon(user.isPatron)
  def lineIcon(user: User)(implicit lang: Lang): Frag                      = lineIcon(user.isPatron)
  def lineIconChar(user: User): Frag                                       = if (user.isPatron) patronIconChar else lineIconChar
}
