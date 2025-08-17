package lila.ui

import scala.annotation.targetName
import chess.{ PlayerTitle, IntRating }
import chess.rating.{ IntRatingDiff, RatingProvisional }

import lila.core.LightUser
import lila.core.perf.{ KeyedPerf, UserPerfs, UserWithPerfs }
import lila.core.socket.IsOnline
import lila.ui.ScalatagsTemplate.{ *, given }

trait UserHelper:
  self: I18nHelper & NumberHelper & AssetHelper =>

  protected val ratingApi: RatingApi
  def isOnline: IsOnline
  def lightUserSync: LightUser.GetterSync

  given Conversion[UserWithPerfs, User] = _.user

  def usernameOrId(userId: UserId): UserName = lightUserSync(userId).fold(userId.into(UserName))(_.name)
  def titleNameOrId(userId: UserId): String = lightUserSync(userId).fold(userId.value)(_.titleName)
  def titleNameOrAnon(userId: Option[UserId]): String =
    userId.flatMap(lightUserSync).fold(UserName.anonymous.value)(_.titleName)

  def titleTag(title: Option[PlayerTitle]): Option[Frag] =
    title.map: t =>
      frag(userTitleTag(t), nbsp)
  def titleTag(lu: LightUser): Frag = titleTag(lu.title)

  def userFlair(user: User): Option[Tag] = user.flair.map(userFlair)
  def userFlair(flair: Flair): Tag = img(cls := "uflair", src := flairSrc(flair))
  def userFlairSync(userId: UserId): Option[Tag] = lightUserSync(userId).flatMap(_.flair).map(userFlair)

  def renderRating(perf: Perf): Frag = frag(" (", perf.intRating, perf.provisional.yes.option("?"), ")")

  // UserPerfs selects the best perf
  def userRating(perf: Perf | UserPerfs): Frag = perf match
    case p: Perf => renderRating(p)
    case p: UserPerfs => ratingApi.bestRated(p).map(_.perf).so(renderRating)

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
      modIcon: Boolean = false,
      withFlair: Boolean = true,
      withPowerTip: Boolean = true
  )(using Translate): Tag =
    userIdOption
      .flatMap(u => lightUserSync(u.id))
      .fold[Tag](anonUserSpan(cssClass, modIcon)): user =>
        userIdNameLink(
          userId = user.id,
          username = user.name,
          isPatron = user.isPatron,
          title = user.title.ifTrue(withTitle),
          flair = if withFlair then user.flair else none,
          cssClass = cssClass,
          withOnline = withOnline,
          truncate = truncate,
          params = params,
          modIcon = modIcon,
          withPowerTip = withPowerTip
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
      withOnline: Boolean = true
  )(using Translate): Tag =
    span(
      cls := userClass(user.id, cssClass, withOnline),
      dataHref := userUrl(user.name)
    )(
      withOnline.so(lineIcon(user.isPatron)),
      titleTag(user.title),
      user.name,
      user.flair.map(userFlair)
    )

  def userLink(
      user: User,
      withOnline: Boolean = true,
      withPowerTip: Boolean = true,
      withTitle: Boolean = true,
      withPerfRating: Option[Perf | UserPerfs] = None,
      name: Option[Frag] = None,
      params: String = "",
      withFlair: Boolean = true
  )(using Translate): Tag =
    a(
      cls := userClass(user.id, none, withOnline, withPowerTip),
      href := userUrl(user.username, params)
    )(userLinkContent(user, withOnline, withTitle, withPerfRating, name, withFlair))

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
      cls := userClass(user.id, cssClass, withOnline, withPowerTip),
      dataHref := userUrl(user.username)
    )(userLinkContent(user, withOnline, withTitle, withPerfRating, name))

  def userLinkContent(
      user: User,
      withOnline: Boolean = true,
      withTitle: Boolean = true,
      withPerfRating: Option[Perf | UserPerfs] = None,
      name: Option[Frag] = None,
      withFlair: Boolean = true
  )(using Translate) = frag(
    withOnline.so(lineIcon(user)),
    withTitle.option(titleTag(user.title)),
    name | user.username,
    withFlair.so(userFlair(user)),
    withPerfRating.map(userRating)
  )

  def userIdNameLink(
      userId: UserId,
      username: UserName,
      isPatron: Boolean,
      cssClass: Option[String],
      withOnline: Boolean,
      truncate: Option[Int],
      title: Option[PlayerTitle],
      flair: Option[Flair],
      params: String,
      modIcon: Boolean,
      withPowerTip: Boolean = true
  )(using Translate): Tag =
    a(
      cls := userClass(userId, cssClass, withOnline, withPowerTip),
      href := userUrl(username, params = params)
    )(
      withOnline.so(if modIcon then moderatorIcon else lineIcon(isPatron)),
      titleTag(title),
      truncate.fold(username.value)(username.value.take),
      flair.map(userFlair)
    )

  def userIdSpanMini(userId: UserId, withOnline: Boolean = false)(using Translate): Tag =
    val user = lightUserSync(userId)
    val name = user.fold(userId.into(UserName))(_.name)
    span(
      cls := userClass(userId, none, withOnline),
      dataHref := userUrl(name)
    )(
      withOnline.so(lineIcon(user)),
      user.map(titleTag),
      name
    )

  def userUrl(username: UserName, params: String = ""): Option[String] =
    Option.when(username.id.noGhost):
      s"${routes.User.show(username)}$params"

  def userClass(
      userId: UserId,
      cssClass: Option[String],
      withOnline: Boolean,
      withPowerTip: Boolean = true
  ): List[(String, Boolean)] =
    if userId.isGhost then List("user-link" -> true, ~cssClass -> cssClass.isDefined)
    else
      (withOnline.so(List((if isOnline.exec(userId) then "online" else "offline") -> true))) ::: List(
        "user-link" -> true,
        ~cssClass -> cssClass.isDefined,
        "ulpt" -> withPowerTip
      )

  def ratingProgress(progress: IntRatingDiff): Option[Frag] =
    if progress.positive then goodTag(cls := "rp")(progress).some
    else if progress.negative then badTag(cls := "rp")(math.abs(progress.value)).some
    else none

  def showPerfRating(
      rating: IntRating,
      name: String,
      nb: Int,
      provisional: RatingProvisional,
      clueless: Boolean,
      icon: Icon
  )(using Translate): Frag =
    span(
      title := trans.site.ratingXOverYGames.pluralTxt(nb, name, nb.localize),
      dataIcon := icon,
      cls := "text"
    )(
      if clueless then frag(nbsp, nbsp, nbsp, if nb < 1 then "-" else "?")
      else frag(rating, provisional.yes.option("?"))
    )

  def showPerfRating(p: KeyedPerf)(using Translate): Frag =
    import p.perf.*
    showPerfRating(
      intRating,
      p.key.perfTrans,
      nb,
      provisional,
      glicko.clueless,
      p.key.perfIcon
    )

  def showPerfRating(perfs: UserPerfs, perfKey: PerfKey)(using Translate): Frag =
    showPerfRating(perfs.keyed(perfKey))

  def showBestPerf(perfs: UserPerfs)(using Translate): Option[Frag] =
    ratingApi.bestRated(perfs).map(showPerfRating)

  def showRatingDiff(diff: IntRatingDiff): Frag = diff.value match
    case 0 => span("±0")
    case d if d > 0 => goodTag(s"+$d")
    case d => badTag(s"−${-d}")

  val patronIconChar = Icon.Wings
  val lineIconChar = Icon.Disc

  val lineIcon: Frag = i(cls := "line")

  def patronIcon(using Translate): Frag =
    i(cls := s"line patron", title := trans.patron.lichessPatron.txt())

  val moderatorIcon: Frag = i(cls := "line moderator", title := "Lichess Mod")
  @targetName("lineIconPatron")
  private def lineIcon(isPatron: Boolean)(using Translate): Frag =
    if isPatron then patronIcon else lineIcon
  @targetName("lineIconUser")
  private def lineIcon(user: Option[LightUser])(using Translate): Frag =
    lineIcon(user.exists(_.isPatron))
  def lineIcon(user: LightUser)(using Translate): Frag = lineIcon(user.isPatron)
  def lineIcon(user: User)(using Translate): Frag = lineIcon(user.isPatron)
  def lineIconChar(user: User): Icon = if user.isPatron then patronIconChar else lineIconChar
