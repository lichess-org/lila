package lila.app
package templating

import chess.PlayerTitle
import controllers.routes

import lila.web.ui.ScalatagsTemplate.{ *, given }
import lila.common.Icon
import lila.core.LightUser
import lila.core.i18n.{ Translate, I18nKey as trans }
import lila.core.perf.{ Perf, UserPerfs, UserWithPerfs }
import lila.rating.PerfType
import lila.app.mashup.*
import lila.common.Icon
import lila.core.user.User
import lila.rating.GlickoExt.clueless
import lila.rating.UserPerfsExt.bestRatedPerf
import lila.core.perf.KeyedPerf
import lila.rating.UserPerfsExt.bestPerfs
import lila.web.ui.*

trait UserHelper:
  self: I18nHelper & StringHelper & DateHelper & AssetHelper =>

  import NumberHelper.*

  def env: Env
  given Conversion[UserWithPerfs, User] = _.user

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
      icon: Icon
  )(using Translate): Frag =
    span(
      title    := trans.site.ratingXOverYGames.pluralTxt(nb, name, nb.localize),
      dataIcon := icon,
      cls      := "text"
    )(
      if clueless then frag(nbsp, nbsp, nbsp, if nb < 1 then "-" else "?")
      else frag(rating, provisional.yes.option("?"))
    )

  def showPerfRating(p: KeyedPerf)(using Translate): Frag =
    import p.*
    showPerfRating(
      perf.intRating,
      PerfType(key).trans,
      perf.nb,
      perf.provisional,
      perf.glicko.clueless,
      PerfType(key).icon
    )

  def showPerfRating(perfs: UserPerfs, perfKey: PerfKey)(using Translate): Frag =
    showPerfRating(perfs.keyed(perfKey))
  def showBestPerf(perfs: UserPerfs)(using Translate): Option[Frag] =
    perfs.bestRatedPerf.map(showPerfRating)
  def showBestPerfs(perfs: UserPerfs, nb: Int)(using Translate): List[Frag] =
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
    userId.flatMap(lightUser).fold(UserName.anonymous.value)(_.titleName)

  def isOnline(userId: UserId) = env.socket.isOnline(userId)

  def isStreaming(userId: UserId) = env.streamer.liveStreamApi.isStreaming(userId)

  def anonUserSpan(cssClass: Option[String] = None, modIcon: Boolean = false) =
    span(cls := List("offline" -> true, "user-link" -> true, ~cssClass -> cssClass.isDefined))(
      if modIcon then frag(moderatorIcon, UserName.anonMod)
      else UserName.anonymous
    )

  def userIdLink[U: UserIdOf](
      userIdOption: Option[U],
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withTitle: Boolean = true,
      truncate: Option[Int] = None,
      params: String = "",
      modIcon: Boolean = false
  )(using Translate): Tag =
    userIdOption
      .flatMap(u => lightUser(u.id))
      .fold[Tag](anonUserSpan(cssClass, modIcon)): user =>
        userIdNameLink(
          userId = user.id,
          username = user.name,
          isPatron = user.isPatron,
          title = user.title.ifTrue(withTitle),
          flair = user.flair,
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
  )(using Translate): Tag =
    userIdNameLink(
      userId = user.id,
      username = user.name,
      isPatron = user.isPatron,
      title = user.title.ifTrue(withTitle),
      flair = user.flair,
      cssClass = cssClass,
      withOnline = withOnline,
      truncate = truncate,
      params = params,
      modIcon = false
    )

  def lightUserSpan(
      user: LightUser,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withTitle: Boolean = true,
      params: String = ""
  )(using Translate): Tag =
    span(
      cls      := userClass(user.id, cssClass, withOnline),
      dataHref := userUrl(user.name)
    )(
      withOnline.so(lineIcon(user.isPatron)),
      titleTag(user.title),
      user.name,
      user.flair.map(userFlair)
    )

  def titleTag(title: Option[PlayerTitle]): Option[Frag] =
    title.map: t =>
      frag(userTitleTag(t), nbsp)
  def titleTag(lu: LightUser): Frag = titleTag(lu.title)

  private def userIdNameLink(
      userId: UserId,
      username: UserName,
      isPatron: Boolean,
      cssClass: Option[String],
      withOnline: Boolean,
      truncate: Option[Int],
      title: Option[PlayerTitle],
      flair: Option[Flair],
      params: String,
      modIcon: Boolean
  )(using Translate): Tag =
    a(
      cls  := userClass(userId, cssClass, withOnline),
      href := userUrl(username, params = params)
    )(
      withOnline.so(if modIcon then moderatorIcon else lineIcon(isPatron)),
      titleTag(title),
      truncate.fold(username.value)(username.value.take),
      flair.map(userFlair)
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
  )(using Translate): Tag =
    a(
      cls  := userClass(user.id, cssClass, withOnline, withPowerTip),
      href := userUrl(user.username, params)
    )(userLinkContent(user, withOnline, withTitle, withPerfRating, name))

  def userSpan(
      user: User,
      cssClass: Option[String] = None,
      withOnline: Boolean = true,
      withPowerTip: Boolean = true,
      withTitle: Boolean = true,
      withPerfRating: Option[Perf | UserPerfs] = None,
      name: Option[Frag] = None
  )(using Translate): Tag =
    span(
      cls      := userClass(user.id, cssClass, withOnline, withPowerTip),
      dataHref := userUrl(user.username)
    )(userLinkContent(user, withOnline, withTitle, withPerfRating, name))

  def userLinkContent(
      user: User,
      withOnline: Boolean = true,
      withTitle: Boolean = true,
      withPerfRating: Option[Perf | UserPerfs] = None,
      name: Option[Frag] = None
  )(using Translate) = frag(
    withOnline.so(lineIcon(user)),
    withTitle.option(titleTag(user.title)),
    name | user.username,
    userFlair(user),
    withPerfRating.map(userRating(user, _))
  )

  def userIdSpanMini(userId: UserId, withOnline: Boolean = false)(using Translate): Tag =
    val user = lightUser(userId)
    val name = user.fold(userId.into(UserName))(_.name)
    span(
      cls      := userClass(userId, none, withOnline),
      dataHref := userUrl(name)
    )(
      withOnline.so(lineIcon(user)),
      user.map(titleTag),
      name
    )

  def userFlair(user: User): Option[Tag] = user.flair.map(userFlair)

  def userFlair(flair: Flair): Tag = img(cls := "uflair", src := flairSrc(flair))

  private def renderRating(perf: Perf): Frag =
    frag(" (", perf.intRating, perf.provisional.yes.option("?"), ")")

  // UserPerfs selects the best perf
  private def userRating(user: User, perf: Perf | UserPerfs): Frag = perf match
    case p: Perf      => renderRating(p)
    case p: UserPerfs => p.bestRatedPerf.so(p => renderRating(p.perf))

  def userUrl(username: UserName, params: String = ""): Option[String] =
    username.id.noGhost.option(s"""${routes.User.show(username.value)}$params""")

  def userClass(
      userId: UserId,
      cssClass: Option[String],
      withOnline: Boolean,
      withPowerTip: Boolean = true
  ): List[(String, Boolean)] =
    if userId.isGhost then List("user-link" -> true, ~cssClass -> cssClass.isDefined)
    else
      (withOnline.so(List((if isOnline(userId) then "online" else "offline") -> true))) ::: List(
        "user-link" -> true,
        ~cssClass   -> cssClass.isDefined,
        "ulpt"      -> withPowerTip
      )

  def userGameFilterTitle(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(using Translate): Frag =
    if filter == GameFilter.Search then frag(iconTag(Icon.Search), br, trans.search.advancedSearch())
    else splitNumber(userGameFilterTitleNoTag(u, nbs, filter))

  private def transLocalize(key: lila.core.i18n.I18nKey, number: Int)(using Translate) =
    key.pluralSameTxt(number)

  def userGameFilterTitleNoTag(u: User, nbs: UserInfo.NbGames, filter: GameFilter)(using Translate): String =
    filter match
      case GameFilter.All      => transLocalize(trans.site.nbGames, u.count.game)
      case GameFilter.Me       => nbs.withMe.so { transLocalize(trans.site.nbGamesWithYou, _) }
      case GameFilter.Rated    => transLocalize(trans.site.nbRated, u.count.rated)
      case GameFilter.Win      => transLocalize(trans.site.nbWins, u.count.win)
      case GameFilter.Loss     => transLocalize(trans.site.nbLosses, u.count.loss)
      case GameFilter.Draw     => transLocalize(trans.site.nbDraws, u.count.draw)
      case GameFilter.Playing  => transLocalize(trans.site.nbPlaying, nbs.playing)
      case GameFilter.Bookmark => transLocalize(trans.site.nbBookmarks, nbs.bookmark)
      case GameFilter.Imported => transLocalize(trans.site.nbImportedGames, nbs.imported)
      case GameFilter.Search   => trans.search.advancedSearch.txt()

  def describeUser(user: UserWithPerfs)(using Translate) =
    val name      = user.titleUsername
    val nbGames   = user.count.game
    val createdAt = showEnglishDate(user.createdAt)
    val currentRating = user.perfs.bestRatedPerf.so: p =>
      s" Current ${PerfType(p.key).trans} rating: ${p.perf.intRating}."
    s"$name played $nbGames games since $createdAt.$currentRating"

  val patronIconChar = Icon.Wings
  val lineIconChar   = Icon.Disc

  val lineIcon: Frag = i(cls := "line")
  def patronIcon(using Translate): Frag =
    i(cls := "line patron", title := trans.patron.lichessPatron.txt())
  val moderatorIcon: Frag = i(cls := "line moderator", title := "Lichess Mod")
  private def lineIcon(patron: Boolean)(using Translate): Frag = if patron then patronIcon else lineIcon
  private def lineIcon(user: Option[LightUser])(using Translate): Frag = lineIcon(user.exists(_.isPatron))
  def lineIcon(user: LightUser)(using Translate): Frag                 = lineIcon(user.isPatron)
  def lineIcon(user: User)(using Translate): Frag                      = lineIcon(user.isPatron)
  def lineIconChar(user: User): Icon = if user.isPatron then patronIconChar else lineIconChar
