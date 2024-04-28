package views.tutor

import lila.app.templating.Environment.{ *, given }

lazy val bits = lila.tutor.ui.TutorBits(helpers)(views.opening.bits.openingUrl)

private[tutor] def layout(
    menu: Frag,
    title: String = "Lichess Tutor",
    pageSmall: Boolean = false
)(content: Modifier*)(using PageContext) =
  views.base.layout(
    moreCss = cssTag("tutor"),
    modules = EsmInit("tutor"),
    title = title,
    csp = defaultCsp.withInlineIconFont.some
  ):
    main(cls := List("page-menu tutor" -> true, "page-small" -> pageSmall))(
      lila.ui.bits.subnav(menu),
      div(cls := "page-menu__content")(content)
    )
