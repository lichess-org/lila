package views.html.tutor

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json._
import scalatags.Text

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.insight.InsightPosition
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

  def peerGrade[A](
      c: Either[String, TutorConcept],
      metricOptions: TutorBothValueOptions[A],
      titleTag: Text.Tag = h3,
      detail: Frag = emptyFrag
  )(implicit lang: Lang, number: TutorNumber[A]) =
    metricOptions.asAvailable map { metric =>
      val grade       = metric.grade
      val minePercent = renderPercent(metric.mine.value)
      val peerPercent = renderPercent(metric.peer.value)
      div(cls := "tutor-grade")(
        titleTag(cls := "tutor-grade__name")(c.fold(concept.show, concept.show)),
        div(
          cls   := s"tutor-grade__visual tutor-grade__visual--${grade.wording.id}",
          title := s"$minePercent% vs $peerPercent%"
        )(
          lila.tutor.Grade.Wording.list.map { gw =>
            div(cls := (grade.wording >= gw).option("lit"))
          }
        ),
        detail
      )
    }

  def peerGradeWithDetail[A: TutorNumber](
      c: TutorConcept,
      metric: TutorBothValueOptions[A],
      position: InsightPosition,
      titleTag: Text.Tag = h3
  )(implicit lang: Lang) =
    peerGrade(
      Right(c),
      metric,
      titleTag = titleTag,
      detail = metric.mine.fold(emptyFrag) { mine =>
        div(cls := "tutor-grade__detail")(
          strong(renderPercent(mine.value)),
          "%",
          metric.peer.map { peer =>
            em(" vs ", strong(renderPercent(peer.value)), "% (peers)")
          },
          " over ",
          mine.count.localize,
          " ",
          position.short
        )
      }
    )

  private def horizontalBarPercent[A](
      value: Option[ValueCount[A]],
      legend: String,
      extraCls: String
  )(implicit lang: Lang, number: TutorNumber[A]) =
    value match {
      case Some(v) =>
        div(
          cls   := s"tutor-bar tutor-bar--$extraCls",
          style := s"--value:${Math.round(number double v.value)}%"
        )(
          span(legend),
          em(strong(renderPercent(v.value)), "%", " (", v.count.localize, ")")
        )
      case None => div(cls := s"tutor-bar tutor-bar--$extraCls tutor-bar--empty")
    }

  private def renderPercent[A](v: A)(implicit number: TutorNumber[A]) = f"${number double v}%1.1f"

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
