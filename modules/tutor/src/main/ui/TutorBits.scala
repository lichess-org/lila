package lila.tutor
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.rating.PerfType
import lila.insight.MeanRating

final class TutorBits(helpers: Helpers)(
    val openingUrl: chess.opening.Opening => Call
):
  import helpers.{ *, given }

  def page(menu: Frag, title: String = "Lichess Tutor", pageSmall: Boolean = false)(mods: AttrPair*): Page =
    Page(title)
      .css("tutor.report")
      .js(Esm("tutor"))
      .csp(_.withInlineIconFont)
      .wrap: body =>
        main(cls := List("page-menu tutor" -> true, "page-small" -> pageSmall))(
          lila.ui.bits.subnav(menu),
          div(cls := "page-menu__content")(mods, body)
        )

  def mascot =
    img(
      cls := "mascot",
      src := assetUrl("images/mascot/octopus-shadow.svg")
    )

  def mascotSays(content: Modifier*) = div(cls := "mascot-says")(
    div(cls := "mascot-says__content")(content),
    mascot
  )

  def dateRange(config: TutorConfig)(print: Instant => Frag) =
    frag(print(config.from), " → ", print(config.to))

  def days(config: TutorConfig)(using Translate) =
    trans.site.nbDays.plural(config.days, strong(config.days.localize))

  def reportTime(config: TutorConfig)(using Translate) =
    span(cls := "tutor-badge tutor-badge--time")(
      span(cls := "tutor-badge__dates")(dateRange(config)(showDateShort(_))),
      span(cls := "tutor-badge__days")(days(config))
    )

  def reportMeta(nbGames: Int, rating: Option[MeanRating])(using Translate) =
    val tag = if nbGames == 0 then badTag else span
    tag(cls := "tutor-badge tutor-badge--meta")(
      span(cls := "tutor-badge__games")(trans.site.nbGames.plural(nbGames, strong(nbGames.localize))),
      span(cls := "tutor-badge__rating")(trans.site.rating(), " ", strong(rating.fold("?")(_.toString)))
    )

  val seeMore = a(cls := "tutor-card__more")("Click to see more...")

  def percentNumber[A](v: A)(using number: TutorNumber[A]) = f"${number.double(v)}%1.1f"
  def percentFrag[A](v: A)(using TutorNumber[A]) = frag(strong(percentNumber(v)), "%")

  def beta = strong(cls := "tutor__beta")("BETA")

  def otherUser(user: UserId)(using ctx: Context) =
    ctx.isnt(user).option(userIdSpanMini(user, withOnline = false))

  def menuBase(report: Option[TutorPerfReport])(using
      config: TutorConfig
  )(using Context): Frag = frag(
    a(href := routes.Tutor.user(config.user))("Tutor"),
    a(href := config.url.root, cls := report.isEmpty.option("active"))(dateRange(config)(showDateShort))
  )

  def menu(full: TutorFullReport, report: Option[TutorPerfReport])(using Context): Frag = frag(
    menuBase(report)(using full.config),
    full.perfs.map: p =>
      a(
        cls := List("active" -> report.exists(_.perf === p.perf)),
        dataIcon := p.perf.icon,
        href := full.url.perf(p.perf)
      )(p.perf.trans)
  )

  def perfSelector(full: TutorFullReport, current: PerfType, angle: Option[Angle])(using
      Context
  ) =
    lila.ui.bits.mselect(
      "tutor-perf-select",
      span(cls := "text", dataIcon := current.icon)(current.trans),
      full.perfs.toList.map: r =>
        a(
          href := full.url.angle(r.perf, angle),
          cls := List("text" -> true, "current" -> (current == r.perf)),
          dataIcon := r.perf.icon
        )(r.perf.trans)
    )

  def reportSelector(report: TutorPerfReport, current: Angle)(using config: TutorConfig) =
    lila.ui.bits.mselect(
      "tutor-report-select",
      span(reportAngles.find(_._1 == current).map(_._2) | current),
      reportAngles.map: (angle, name) =>
        a(
          href := config.url.angle(report.perf, angle),
          cls := (current == angle).option("current")
        )(name)
    )

  val reportAngles: List[(Angle, String)] = List(
    ("skills", "Skills"),
    ("opening", "Openings"),
    ("time", "Time management"),
    ("phases", "Game phases"),
    ("pieces", "Pieces")
  )
