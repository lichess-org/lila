package lila.tutor
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }
import lila.rating.PerfType

final class TutorBits(helpers: Helpers)(
    val openingUrl: chess.opening.Opening => Call
):
  import helpers.{ *, given }

  def page(menu: Frag, title: String = "Lichess Tutor", pageSmall: Boolean = false)(mods: AttrPair*) =
    Page(title)
      .css("tutor")
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

  val seeMore = a(cls := "tutor-card__more")("Click to see more...")

  def percentNumber[A](v: A)(using number: TutorNumber[A]) = f"${number.double(v)}%1.1f"
  def percentFrag[A](v: A)(using TutorNumber[A]) = frag(strong(percentNumber(v)), "%")

  def beta = strong(cls := "tutor__beta")("BETA")

  def otherUser(user: User)(using ctx: Context) =
    ctx.isnt(user).option(userSpan(user, withOnline = false, withTitle = false, withFlair = false))

  def menu(full: TutorFullReport.Available, user: User, report: Option[TutorPerfReport])(using
      Context
  ) = frag(
    a(href := routes.Tutor.user(user.username), cls := report.isEmpty.option("active"))("Tutor"),
    full.report.perfs.map: p =>
      a(
        cls := List("text" -> true, "active" -> report.exists(_.perf === p.perf)),
        dataIcon := p.perf.icon,
        href := routes.Tutor.perf(user.username, p.perf.key)
      )(p.perf.trans)
  )

  def perfSelector(full: TutorFullReport, current: PerfType)(routing: (UserStr, PerfKey) => Call)(using
      Context
  ) =
    lila.ui.bits.mselect(
      "tutor-perf-select",
      span(cls := "text", dataIcon := current.icon)(current.trans),
      full.perfs.toList.map: r =>
        a(
          href := routing(usernameOrId(full.user), r.perf.key),
          cls := List("text" -> true, "current" -> (current == r.perf)),
          dataIcon := r.perf.icon
        )(r.perf.trans)
    )

  def reportSelector(report: TutorPerfReport, current: String, user: User) =
    lila.ui.bits.mselect(
      "tutor-report-select",
      span(reportAngles.find(_._1 == current).map(_._2) | current),
      reportAngles.map: (key, name, route) =>
        a(
          href := route(user.username, report.perf.key),
          cls := (current == key).option("current")
        )(name)
    )

  val reportAngles: List[(String, String, (UserStr, PerfKey) => Call)] = List(
    ("skills", "Skills", routes.Tutor.skills),
    ("openings", "Openings", routes.Tutor.openings),
    ("time", "Time management", routes.Tutor.time),
    ("phases", "Game phases", routes.Tutor.phases)
  )
