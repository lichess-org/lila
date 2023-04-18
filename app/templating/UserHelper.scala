package lila.app
package templating

import controllers.routes
import mashup.*
import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.LightUser
import lila.i18n.{ I18nKey, I18nKeys as trans }
import lila.rating.{ Perf, PerfType }
import lila.user.User

trait UserHelper extends HasEnv { self: I18nHelper with StringHelper with NumberHelper with DateHelper =>

  def ratingProgress(progress: IntRatingDiff): Option[Frag] =
    if (progress > 0) goodTag(cls := "rp")(progress).some
    else if (progress < 0) badTag(cls := "rp")(math.abs(progress.value)).some
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

  def showPerfRating(
      rating: IntRating,
      name: String,
      nb: Int,
      provisional: RatingProvisional,
      clueless: Boolean,
      icon: Char
  )(using Lang): Frag =
    span(
      title    := s"$name rating over ${nb.localize} games",
      dataIcon := icon,
      cls      := "text"
    )(
      if (clueless) frag(nbsp, nbsp, nbsp, if (nb < 1) "-" else "?")
      else frag(rating, provisional.yes option "?")
    )

  def showPerfRating(perfType: PerfType, perf: Perf)(using Lang): Frag =
    showPerfRating(
      perf.intRating,
      perfType.trans,
      perf.nb,
      perf.provisional,
      perf.clueless,
      perfType.iconChar
    )

  def showPerfRating(u: User, perfType: PerfType)(using Lang): Frag =
    showPerfRating(perfType, u perfs perfType)

  def showPerfRating(u: User, perfKey: Perf.Key)(using Lang): Option[Frag] =
    PerfType(perfKey) map { showPerfRating(u, _) }

  def showBestPerf(u: User)(using Lang): Option[Frag] =
    u.perfs.bestPerf map { case (pt, perf) =>
      showPerfRating(pt, perf)
    }
  def showBestPerfs(u: User, nb: Int)(using Lang): List[Frag] =
    u.perfs.bestPerfs(nb) map { case (pt, perf) =>
      showPerfRating(pt, perf)
    }

  def showRatingDiff(diff: IntRatingDiff): Frag = diff.value match
    case 0          => span("±0")
    case d if d > 0 => goodTag(s"+$d")
    case d          => badTag(s"−${-d}")

  inline def lightUser         = env.user.lightUserSync
  inline def lightUserFallback = env.user.lightUserSyncFallback

  def usernameOrId(userId: UserId): String  = lightUser(userId).fold(userId.value)(_.name.value)
  def titleNameOrId(userId: UserId): String = lightUser(userId).fold(userId.value)(_.titleName)
  def titleNameOrAnon(userId: Option[UserId]): String =
    userId.flatMap(lightUser).fold(User.anonymous.value)(_.titleName)

  def isOnline(userId: UserId) = env.socket.isOnline(userId)

  def isStreaming(userId: UserId) = env.streamer.liveStreamApi isStreaming userId

  def anonUserSpan(cssClass: Option[String] = None, modIcon: Boolean = false) =
    span(cls := List("offline" -> true, "user-link" -> true, ~cssClass -> cssClass.isDefined))(
      if (modIcon) frag(moderatorIcon, User.anonMod)
      else User.anonymous
    )

  def userIdLink[U](
      userIdOption: Option[U],
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withTitle: Boolean = true,
      truncate: Option[Int] = None,
      params: String = "",
      modIcon: Boolean = false
  )(using Lang)(using idOf: UserIdOf[U]): Tag =
    userIdOption.flatMap(u => lightUser(idOf(u))).fold[Tag](anonUserSpan(cssClass, modIcon)) { user =>
      userIdNameLink(
        userId = user.id,
        username = user.name,
        isPatron = user.isPatron,
        title = withTitle ?? user.title,
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
  )(using lang: Lang): Tag =
    userIdNameLink(
      userId = user.id,
      username = user.name,
      isPatron = user.isPatron,
      title = withTitle ?? user.title,
      cssClass = cssClass,
      withOnline = withOnline,
      truncate = truncate,
      params = params,
      modIcon = false
    )

  def titleTag(title: Option[UserTitle]): Option[Frag] =
    title map { t =>
      frag(userTitleTag(t), nbsp)
    }
  def titleTag(lu: LightUser): Frag = titleTag(lu.title)

  private def userIdNameLink(
      userId: UserId,
      username: UserName,
      isPatron: Boolean,
      cssClass: Option[String],
      withOnline: Boolean,
      truncate: Option[Int],
      title: Option[UserTitle],
      params: String,
      modIcon: Boolean
  )(using lang: Lang): Tag =
    a(
      cls  := userClass(userId, cssClass, withOnline),
      href := userUrl(username, params = params)
    )(
      withOnline ?? (if (modIcon) moderatorIcon else lineIcon(isPatron)),
      titleTag(title),
      truncate.fold(username.value)(username.value.take)
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
  )(using lang: Lang): Tag =
    a(
      cls  := userClass(user.id, cssClass, withOnline, withPowerTip),
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
  )(using lang: Lang): Frag =
    span(
      cls      := userClass(user.id, cssClass, withOnline, withPowerTip),
      dataHref := userUrl(user.username)
    )(
      withOnline ?? lineIcon(user),
      withTitle option titleTag(user.title),
      name | user.username,
      userRating(user, withPerfRating, withBestRating)
    )

  def userIdSpanMini(userId: UserId, withOnline: Boolean = false)(using lang: Lang): Tag =
    val user = lightUser(userId)
    val name = user.fold(userId into UserName)(_.name)
    span(
      cls      := userClass(userId, none, withOnline),
      dataHref := userUrl(name)
    )(
      withOnline ?? lineIcon(user),
      user.map(titleTag),
      name
    )

  private def renderRating(perf: Perf): Frag =
    frag(
      " (",
      perf.intRating,
      perf.provisional.yes option "?",
      ")"
    )

  private def userRating(user: User, withPerfRating: Option[PerfType], withBestRating: Boolean): Frag =
    withPerfRating match
      case Some(perfType) => renderRating(user.perfs(perfType))
      case _ if withBestRating =>
        user.perfs.bestPerf ?? { case (_, perf) =>
          renderRating(perf)
        }
      case _ => ""

  private def userUrl(username: UserName, params: String = ""): Option[String] =
    !User.isGhost(username.id) option s"""${routes.User.show(username.value)}$params"""

  def userClass(
      userId: UserId,
      cssClass: Option[String],
      withOnline: Boolean,
      withPowerTip: Boolean = true
  ): List[(String, Boolean)] =
    if (User isGhost userId) List("user-link" -> true, ~cssClass -> cssClass.isDefined)
    else
      (withOnline ?? List((if (isOnline(userId)) "online" else "offline") -> true)) ::: List(
        "user-link" -> true,
        ~cssClass   -> cssClass.isDefined,
        "ulpt"      -> withPowerTip
      )

  def userGameFilterTitle(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(using
      lang: Lang
  ): Frag =
    if (filter == GameFilter.Search) frag(iconTag(""), br, trans.search.advancedSearch())
    else splitNumber(userGameFilterTitleNoTag(u, nbs, filter))

  private def transLocalize(key: I18nKey, number: Int)(using lang: Lang) =
    key.pluralSameTxt(number)

  def userGameFilterTitleNoTag(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(using
      lang: Lang
  ): String =
    filter match
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

  def describeUser(user: User)(using lang: Lang) =
    val name      = user.titleUsername
    val nbGames   = user.count.game
    val createdAt = showEnglishDate(user.createdAt)
    val currentRating = user.perfs.bestPerf ?? { (pt, perf) =>
      s" Current ${pt.trans} rating: ${perf.intRating}."
    }
    s"$name played $nbGames games since $createdAt.$currentRating"

  val patronIconChar = ""
  val lineIconChar   = ""

  val lineIcon: Frag = i(cls := "line")
  def patronIcon(using lang: Lang): Frag =
    i(cls := "line patron", title := trans.patron.lichessPatron.txt())
  val moderatorIcon: Frag = i(cls := "line moderator", title := "Lichess Mod")
  private def lineIcon(patron: Boolean)(using lang: Lang): Frag         = if (patron) patronIcon else lineIcon
  private def lineIcon(user: Option[LightUser])(using lang: Lang): Frag = lineIcon(user.??(_.isPatron))
  def lineIcon(user: LightUser)(using lang: Lang): Frag                 = lineIcon(user.isPatron)
  def lineIcon(user: User)(using lang: Lang): Frag                      = lineIcon(user.isPatron)
  def lineIconChar(user: User): Frag = if (user.isPatron) patronIconChar else lineIconChar
}
