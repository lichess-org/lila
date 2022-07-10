package views.html.tutor

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json._
import scalatags.Text

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ Rating, TutorBothValueOptions, TutorFullReport, TutorNumber, ValueCount }

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

  def peerComparison[A: TutorNumber](
      c: TutorConcept,
      metric: TutorBothValueOptions[A],
      titleTag: Text.Tag = h3
  )(implicit lang: Lang) =
    metric.mine map { mine =>
      div(cls := "tutor-comparison")(
        titleTag(cls := "tutor-comparison__name")(concept.show(c)),
        div(cls := "tutor-comparison__unit")(horizontalBarPercent(mine.some, "Yours", "mine")),
        div(cls := "tutor-comparison__unit")(horizontalBarPercent(metric.peer, "Peers", "peer"))
      )
    }

  def peerGrade[A](
      c: TutorConcept,
      metricOptions: TutorBothValueOptions[A],
      titleTag: Text.Tag = h3
  )(implicit lang: Lang, number: TutorNumber[A]) =
    metricOptions.asAvailable map { metric =>
      val grade       = metric.grade
      val minePercent = number.iso.to(metric.mine.value)
      val peerPercent = number.iso.to(metric.peer.value)
      div(cls := "tutor-grade")(
        titleTag(cls := "tutor-grade__name")(concept.show(c)),
        div(cls := "tutor-grade__visual", title := s"$minePercent% vs $peerPercent%")(
          lila.tutor.Grade.Wording.list.map { gw =>
            div(cls := (grade.wording > gw).option("lit"))
          }
        )
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
      pageSmall: Boolean = false
  )(
      content: Modifier*
  )(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("tutor"),
      moreJs = jsModule("tutor"),
      title = "Lichess Tutor"
    ) {
      main(cls := List("page-menu tutor" -> true, "page-small" -> pageSmall))(
        st.aside(cls := "page-menu__menu subnav")(menu),
        div(cls := "page-menu__content")(content)
      )
    }
}
