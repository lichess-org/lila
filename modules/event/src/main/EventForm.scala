package lila.event

import play.api.data.*
import play.api.data.Forms.*
import play.api.i18n.Lang

import lila.common.Form.UTCDate.*
import lila.common.Form.{ stringIn, into }
import lila.i18n.LangList
import lila.user.User

object EventForm:

  object icon:
    val default   = ""
    val broadcast = "broadcast.icon"
    val choices = List(
      default               -> "Microphone",
      "lichess.event.png"   -> "Lichess",
      "trophy.event.png"    -> "Trophy",
      broadcast             -> "Broadcast",
      "offerspill.logo.png" -> "Offerspill"
    )

  val form = Form(
    mapping(
      "title"         -> text(minLength = 3, maxLength = 40),
      "headline"      -> text(minLength = 5, maxLength = 30),
      "description"   -> optional(text(minLength = 5, maxLength = 4000).into[Markdown]),
      "homepageHours" -> bigDecimal(10, 2).verifying(d => d >= 0 && d <= 24),
      "url"           -> nonEmptyText,
      "lang"          -> text.verifying(l => LangList.allChoices.exists(_._1 == l)),
      "enabled"       -> boolean,
      "startsAt"      -> utcDate,
      "finishesAt"    -> utcDate,
      "hostedBy"      -> optional(lila.user.UserForm.historicalUsernameField),
      "icon"          -> stringIn(icon.choices),
      "countdown"     -> boolean
    )(Data.apply)(unapply)
  ) fill Data(
    title = "",
    headline = "",
    description = none,
    homepageHours = 0,
    url = "",
    lang = lila.i18n.enLang.code,
    enabled = true,
    startsAt = nowDate,
    finishesAt = nowDate,
    countdown = true
  )

  case class Data(
      title: String,
      headline: String,
      description: Option[Markdown],
      homepageHours: BigDecimal,
      url: String,
      lang: String,
      enabled: Boolean,
      startsAt: DateTime,
      finishesAt: DateTime,
      hostedBy: Option[UserStr] = None,
      icon: String = "",
      countdown: Boolean
  ):

    def update(event: Event, by: User) =
      event.copy(
        title = title,
        headline = headline,
        description = description,
        homepageHours = homepageHours.toDouble,
        url = url,
        lang = Lang(lang),
        enabled = enabled,
        startsAt = startsAt,
        finishesAt = finishesAt,
        hostedBy = hostedBy.map(_.id),
        icon = icon.some.filter(_.nonEmpty),
        countdown = countdown,
        updatedAt = nowDate.some,
        updatedBy = by.id.some
      )

    def make(userId: UserId) =
      Event(
        _id = Event.makeId,
        title = title,
        headline = headline,
        description = description,
        homepageHours = homepageHours.toDouble,
        url = url,
        lang = Lang(lang),
        enabled = enabled,
        startsAt = startsAt,
        finishesAt = finishesAt,
        createdBy = userId,
        createdAt = nowDate,
        updatedAt = none,
        updatedBy = none,
        hostedBy = hostedBy.map(_.id),
        icon = icon.some.filter(_.nonEmpty),
        countdown = countdown
      )

  object Data:

    def make(event: Event) =
      Data(
        title = event.title,
        headline = event.headline,
        description = event.description,
        homepageHours = event.homepageHours,
        url = event.url,
        lang = event.lang.code,
        enabled = event.enabled,
        startsAt = event.startsAt,
        finishesAt = event.finishesAt,
        hostedBy = event.hostedBy.map(_ into UserStr),
        icon = ~event.icon,
        countdown = event.countdown
      )
