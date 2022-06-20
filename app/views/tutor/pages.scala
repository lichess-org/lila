package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ TutorQueue, TutorFullReport }
import lila.user.User

object pages {

  def empty(availability: TutorFullReport.Empty, user: User)(implicit ctx: Context) =
    bits.layout(availability, menu = emptyFrag, pageSmall = true)(
      cls := "tutor__empty box",
      h1("Lichess Tutor"),
      bits.mascotSays("Explain what tutor is about here."),
      availability.status match {
        case TutorQueue.NotInQueue =>
          postForm(cls := "tutor__empty__cta", action := routes.Tutor.refresh(user.username))(
            submitButton(cls := "button button-fat")("Analyse my games and help me improve")
          )
        case in: TutorQueue.InQueue =>
          frag(
            p("Tutor will analyse your games as soon as possible"),
            p("Position in the queue: ", in.position),
            p("Estimated wait time: ", showMinutes(in.eta.toMinutes.toInt atLeast 1))
          )
      }
    )

  def insufficientGames(implicit ctx: Context) =
    bits.layout(TutorFullReport.InsufficientGames, menu = emptyFrag, pageSmall = true)(
      cls := "tutor__insufficient box",
      h1("Lichess Tutor"),
      bits.mascotSays("Not enough games to analyse! Go and play some more chess.")
    )
}
