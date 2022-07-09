package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ Rating, TutorBothValueOptions, TutorFullReport, TutorNumber, ValueCount }
import play.api.i18n.Lang

object bits {

  val mascot =
    img(
      cls := "tutor__mascot",
      src := assetUrl("images/mascot/octopus-shadow.svg")
    )

  def mascotSays(content: Modifier*) = div(cls := "tutor__mascot-says")(
    div(cls := "tutor__mascot-says__content")(content),
    mascot
  )

  val seeMore = a(cls := "tutor-card__more")("Click to see more...")

  def peerComparison[A: TutorNumber](c: TutorConcept, metric: TutorBothValueOptions[A])(implicit
      lang: Lang
  ) =
    metric.mine map { mine =>
      div(cls := "tutor-comparison")(
        h3(cls := "tutor-comparison__name")(concept.show(c)),
        div(cls := "tutor-comparison__unit")(horizontalBarPercent(mine.some, "Yours", "mine")),
        div(cls := "tutor-comparison__unit")(horizontalBarPercent(metric.peer, "Peers", "peer"))
      )
    }

  private def horizontalBarPercent[A](
      value: Option[ValueCount[A]],
      legend: String,
      extraCls: String
  )(implicit lang: Lang, number: TutorNumber[A]) =
    value match {
      case Some(v) =>
        val double = number.iso.to(v.value)
        div(cls := s"tutor-bar tutor-bar--$extraCls", style := s"--value:${Math.round(double)}%")(
          span(legend),
          em(strong(f"${double}%1.1f"), "%", " (", v.count.localize, ")")
        )
      case None => div(cls := s"tutor-bar tutor-bar--$extraCls tutor-bar--empty")
    }

  private[tutor] def layout(
      availability: TutorFullReport.Availability,
      menu: Frag,
      title: String = "Lichess Tutor",
      pageSmall: Boolean = false,
      moreJs: Frag = emptyFrag
  )(
      content: Modifier*
  )(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("tutor"),
      moreJs = moreJs,
      title = "Lichess Tutor"
    ) {
      main(cls := List("page-menu tutor" -> true, "page-small" -> pageSmall))(
        st.aside(cls := "page-menu__menu subnav")(menu),
        div(cls := "page-menu__content")(content)
      )
    }
}
