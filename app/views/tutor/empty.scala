package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ TutorFullReport, TutorQueue }
import lila.user.User

object empty {

  def start(user: User)(implicit ctx: Context) =
    bits.layout(TutorFullReport.Empty(TutorQueue.NotInQueue), menu = emptyFrag, pageSmall = true)(
      cls := "tutor__empty box",
      h1("Lichess Tutor"),
      bits.mascotSays("Explain what tutor is about here."),
      postForm(cls := "tutor__empty__cta", action := routes.Tutor.refresh(user.username))(
        submitButton(cls := "button button-fat")("Analyse my games and help me improve")
      )
    )

  def queued(in: TutorQueue.InQueue, user: User)(implicit ctx: Context) =
    bits.layout(
      TutorFullReport.Empty(in),
      menu = emptyFrag,
      title = "Lichess Tutor - Examining games...",
      pageSmall = true
    )(
      data("eta") := (in.avgDuration.toMillis atMost 60_000 atLeast 10_000),
      cls         := "tutor__empty tutor__queued box",
      h1("Lichess Tutor"),
      if (in.position == 1)
        bits.mascotSays(
          p(strong(cls := "tutor__intro")("I'm examining your games now!")),
          examinationMethod,
          p("It should be done in a minute or two.")
        )
      else
        bits.mascotSays(
          p(strong(cls := "tutor__intro")("I will examine your games as soon as possible.")),
          examinationMethod,
          p(
            "There are ",
            (in.position - 1),
            " players in the queue before you. ",
            "You will get your results in ",
            showMinutes(in.eta.toMinutes.toInt atLeast 1),
            "."
          )
        ),
      spinner
    )

  private def examinationMethod = frag(
    p(
      "Using the best chess engine: ",
      views.html.plan.features.engineFullName,
      ", ",
      "and comparing your playstyle to thousands of other players with similar rating."
    )
  )

  def insufficientGames(implicit ctx: Context) =
    bits.layout(TutorFullReport.InsufficientGames, menu = emptyFrag, pageSmall = true)(
      cls := "tutor__insufficient box",
      h1("Lichess Tutor"),
      mascotSaysInsufficient
    )

  def mascotSaysInsufficient =
    bits.mascotSays("Not enough rated games to examine! Go and play some more chess.")
}
