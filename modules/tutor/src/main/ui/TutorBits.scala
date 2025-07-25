package lila.tutor
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }

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

  def otherUser(user: User)(using ctx: Context) =
    ctx.isnt(user).option(userSpan(user, withOnline = false))

  def menu(full: TutorFullReport.Available, user: User, report: Option[TutorPerfReport])(using
      Context
  ) = frag(
    a(href := routes.Tutor.user(user.username), cls := report.isEmpty.option("active"))("Tutor"),
    full.report.perfs.map: p =>
      a(
        cls := p.perf.key.value.active(report.so(_.perf.key.value)),
        href := routes.Tutor.perf(user.username, p.perf.key)
      )(p.perf.trans)
  )
