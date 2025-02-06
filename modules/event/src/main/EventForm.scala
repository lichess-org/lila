package lila.event

import play.api.data.*
import play.api.data.Forms.*
import scalalib.model.Language

import lila.common.Form.{ PrettyDateTime, into, stringIn }
import lila.core.i18n.LangList

final class EventForm(langList: LangList):

  import EventForm.*

  val form = Form(
    mapping(
      "title"         -> text(minLength = 3, maxLength = 40),
      "headline"      -> text(minLength = 5, maxLength = 30),
      "description"   -> optional(text(minLength = 5, maxLength = 4000).into[Markdown]),
      "homepageHours" -> bigDecimal(10, 2).verifying(d => d >= 0 && d <= 24),
      "url"           -> nonEmptyText,
      "lang"          -> langList.popularLanguagesForm.mapping,
      "enabled"       -> boolean,
      "startsAt"      -> PrettyDateTime.mapping,
      "finishesAt"    -> PrettyDateTime.mapping,
      "hostedBy"      -> optional(lila.common.Form.username.historicalField),
      "icon"          -> stringIn(icon.choices),
      "countdown"     -> boolean
    )(Data.apply)(unapply)
  ).fill(
    Data(
      title = "",
      headline = "",
      description = none,
      homepageHours = 0,
      url = "",
      lang = lila.core.i18n.defaultLanguage,
      enabled = true,
      startsAt = nowDateTime,
      finishesAt = nowDateTime,
      countdown = true
    )
  )

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

  case class Data(
      title: String,
      headline: String,
      description: Option[Markdown],
      homepageHours: BigDecimal,
      url: String,
      lang: Language,
      enabled: Boolean,
      startsAt: LocalDateTime,
      finishesAt: LocalDateTime,
      hostedBy: Option[UserStr] = None,
      icon: String = "",
      countdown: Boolean
  ):

    def update(event: Event)(using me: MyId) =
      event.copy(
        title = title,
        headline = headline,
        description = description,
        homepageHours = homepageHours.toDouble,
        url = url,
        lang = lang,
        enabled = enabled,
        startsAt = startsAt.instant,
        finishesAt = finishesAt.instant,
        hostedBy = hostedBy.map(_.id),
        icon = icon.some.filter(_.nonEmpty),
        countdown = countdown,
        updatedAt = nowInstant.some,
        updatedBy = me.some
      )

    def make(using me: MyId) =
      Event(
        _id = Event.makeId,
        title = title,
        headline = headline,
        description = description,
        homepageHours = homepageHours.toDouble,
        url = url,
        lang = lang,
        enabled = enabled,
        startsAt = startsAt.instant,
        finishesAt = finishesAt.instant,
        createdBy = me,
        createdAt = nowInstant,
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
        lang = event.lang,
        enabled = event.enabled,
        startsAt = event.startsAt.dateTime,
        finishesAt = event.finishesAt.dateTime,
        hostedBy = event.hostedBy.map(_.into(UserStr)),
        icon = ~event.icon,
        countdown = event.countdown
      )
