package lila.app
package templating

import controllers.routes
import mashup.*
import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.licon
import lila.common.LightUser
import lila.i18n.{ I18nKey, I18nKeys as trans }
import lila.rating.{ Perf, PerfType }
import lila.user.{ User, UserPerfs }

trait UserHelper extends HasEnv:
  self: I18nHelper with StringHelper with NumberHelper with DateHelper =>

  given Conversion[User.WithPerfs, User] = _.user

  def ratingProgress(progress: IntRatingDiff): Option[Frag] =
    if progress > 0 then goodTag(cls := "rp")(progress).some
    else if progress < 0 then badTag(cls := "rp")(math.abs(progress.value)).some
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
      icon: licon.Icon
  )(using Lang): Frag =
    span(
      title    := trans.ratingXOverYGames.pluralTxt(nb, name, nb.localize),
      dataIcon := icon,
      cls      := "text"
    )(
      if clueless then frag(nbsp, nbsp, nbsp, if nb < 1 then "-" else "?")
      else frag(rating, provisional.yes option "?")
    )

  def showPerfRating(p: Perf.Typed)(using Lang): Frag =
    import p.*
    showPerfRating(
      perf.intRating,
      perfType.trans,
      perf.nb,
      perf.provisional,
      perf.clueless,
      perfType.icon
    )

  def showPerfRating(perfs: UserPerfs, perfType: PerfType)(using Lang): Frag =
    showPerfRating(perfs typed perfType)

  def showPerfRating(perfs: UserPerfs, perfKey: Perf.Key)(using Lang): Option[Frag] =
    PerfType(perfKey).map(showPerfRating(perfs, _))

  def showBestPerf(perfs: UserPerfs)(using Lang): Option[Frag] =
    perfs.bestRatedPerf.map(showPerfRating)
  def showBestPerfs(perfs: UserPerfs, nb: Int)(using Lang): List[Frag] =
    perfs.bestPerfs(nb).map(showPerfRating)

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
      if modIcon then frag(moderatorIcon, User.anonMod)
      else User.anonymous
    )

  def userIdLink[U: UserIdOf](
      userIdOption: Option[U],
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withTitle: Boolean = true,
      truncate: Option[Int] = None,
      params: String = "",
      modIcon: Boolean = false
  )(using Lang): Tag =
    userIdOption
      .flatMap(u => lightUser(u.id))
      .fold[Tag](anonUserSpan(cssClass, modIcon)): user =>
        userIdNameLink(
          userId = user.id,
          username = user.name,
          isPatron = user.isPatron,
          title = withTitle so user.title,
          cssClass = cssClass,
          withOnline = withOnline,
          truncate = truncate,
          params = params,
          modIcon = modIcon
        )

  def lightUserLink(
      user: LightUser,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withTitle: Boolean = true,
      truncate: Option[Int] = None,
      params: String = ""
  )(using Lang): Tag =
    userIdNameLink(
      userId = user.id,
      username = user.name,
      isPatron = user.isPatron,
      title = withTitle so user.title,
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
  )(using Lang): Tag =
    a(
      cls  := userClass(userId, cssClass, withOnline),
      href := userUrl(username, params = params)
    )(
      withOnline so (if modIcon then moderatorIcon else lineIcon(isPatron)),
      titleTag(title),
      truncate.fold(username.value)(username.value.take)
    )

  def userLink(
      user: User,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withPowerTip: Boolean = true,
      withTitle: Boolean = true,
      withPerfRating: Option[Perf | UserPerfs] = None,
      name: Option[Frag] = None,
      params: String = ""
  )(using Lang): Tag =
    a(
      cls  := userClass(user.id, cssClass, withOnline, withPowerTip),
      href := userUrl(user.username, params)
    )(
      withOnline so lineIcon(user),
      withTitle option titleTag(user.title),
      name | user.username,
      withPerfRating.map(userRating(user, _))
    )

  def userSpan(
      user: User,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withPowerTip: Boolean = true,
      withTitle: Boolean = true,
      withPerfRating: Option[Perf | UserPerfs] = None,
      name: Option[Frag] = None
  )(using Lang): Frag =
    span(
      cls      := userClass(user.id, cssClass, withOnline, withPowerTip),
      dataHref := userUrl(user.username)
    )(
      withOnline so lineIcon(user),
      withTitle option titleTag(user.title),
      name | user.username,
      withPerfRating.map(userRating(user, _))
    )

  def userIdSpanMini(userId: UserId, withOnline: Boolean = false)(using Lang): Tag =
    val user = lightUser(userId)
    val name = user.fold(userId into UserName)(_.name)
    span(
      cls      := userClass(userId, none, withOnline),
      dataHref := userUrl(name)
    )(
      withOnline so lineIcon(user),
      user.map(titleTag),
      name
    )

  private def renderRating(perf: Perf): Frag =
    frag(" (", perf.intRating, perf.provisional.yes option "?", ")")

  // UserPerfs selects the best perf
  private def userRating(user: User, perf: Perf | UserPerfs): Frag = perf match
    case p: Perf      => renderRating(p)
    case p: UserPerfs => p.bestRatedPerf.so(p => renderRating(p.perf))

  private def userUrl(username: UserName, params: String = ""): Option[String] =
    !User.isGhost(username.id) option s"""${routes.User.show(username.value)}$params"""

  def userClass(
      userId: UserId,
      cssClass: Option[String],
      withOnline: Boolean,
      withPowerTip: Boolean = true
  ): List[(String, Boolean)] =
    if User isGhost userId then List("user-link" -> true, ~cssClass -> cssClass.isDefined)
    else
      (withOnline so List((if isOnline(userId) then "online" else "offline") -> true)) ::: List(
        "user-link" -> true,
        ~cssClass   -> cssClass.isDefined,
        "ulpt"      -> withPowerTip
      )

  def userGameFilterTitle(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(using Lang): Frag =
    if filter == GameFilter.Search then frag(iconTag(licon.Search), br, trans.search.advancedSearch())
    else splitNumber(userGameFilterTitleNoTag(u, nbs, filter))

  private def transLocalize(key: I18nKey, number: Int)(using Lang) = key.pluralSameTxt(number)

  def userGameFilterTitleNoTag(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(using Lang): String =
    filter match
      case GameFilter.All      => transLocalize(trans.nbGames, u.count.game)
      case GameFilter.Me       => nbs.withMe so { transLocalize(trans.nbGamesWithYou, _) }
      case GameFilter.Rated    => transLocalize(trans.nbRated, u.count.rated)
      case GameFilter.Win      => transLocalize(trans.nbWins, u.count.win)
      case GameFilter.Loss     => transLocalize(trans.nbLosses, u.count.loss)
      case GameFilter.Draw     => transLocalize(trans.nbDraws, u.count.draw)
      case GameFilter.Playing  => transLocalize(trans.nbPlaying, nbs.playing)
      case GameFilter.Bookmark => transLocalize(trans.nbBookmarks, nbs.bookmark)
      case GameFilter.Imported => transLocalize(trans.nbImportedGames, nbs.imported)
      case GameFilter.Search   => trans.search.advancedSearch.txt()

  def describeUser(user: User.WithPerfs)(using Lang) =
    val name      = user.titleUsername
    val nbGames   = user.count.game
    val createdAt = showEnglishDate(user.createdAt)
    val currentRating = user.perfs.bestRatedPerf.so: p =>
      s" Current ${p.perfType.trans} rating: ${p.perf.intRating}."
    s"$name played $nbGames games since $createdAt.$currentRating"

  val patronIconChar = licon.Wings
  val lineIconChar   = licon.Disc

  val lineIcon: Frag = i(cls := "line")
  def patronIcon(using Lang): Frag =
    i(cls := "line patron", title := trans.patron.lichessPatron.txt())
  val moderatorIcon: Frag                                 = i(cls := "line moderator", title := "Lichess Mod")
  private def lineIcon(patron: Boolean)(using Lang): Frag = if patron then patronIcon else lineIcon
  private def lineIcon(user: Option[LightUser])(using Lang): Frag = lineIcon(user.so(_.isPatron))
  def lineIcon(user: LightUser)(using Lang): Frag                 = lineIcon(user.isPatron)
  def lineIcon(user: User)(using Lang): Frag                      = lineIcon(user.isPatron)
  def lineIconChar(user: User): Frag = if user.isPatron then patronIconChar else lineIconChar
