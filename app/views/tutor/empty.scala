package views.html.tutor

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.tutor.TutorQueue
import lila.user.User
import lila.game.Pov
import play.api.i18n.Lang
import chess.format.pgn.PgnStr

object empty:

  def start(user: User)(using WebContext) =
    bits.layout(menu = emptyFrag, pageSmall = true)(
      cls := "tutor__empty box",
      boxTop(h1(bits.otherUser(user), "Lichess Tutor")),
      bits.mascotSays("Explain what tutor is about here."),
      postForm(cls := "tutor__empty__cta", action := routes.Tutor.refresh(user.username))(
        submitButton(cls := "button button-fat button-no-upper")("Analyse my games and help me improve")
      )
    )

  def queued(in: TutorQueue.InQueue, user: User, waitGames: List[(Pov, PgnStr)])(using WebContext) =
    bits.layout(
      menu = emptyFrag,
      title = "Lichess Tutor - Examining games...",
      pageSmall = true
    )(
      cls := "tutor__empty tutor__queued box",
      boxTop(h1(bits.otherUser(user), "Lichess Tutor")),
      bits.mascotSays(
        p(strong(cls := "tutor__intro")("I'm examining your games.")),
        examinationMethod,
        nbGames(user),
        p(
          "There are ",
          (in.position - 1),
          " players in the queue before you.",
          br,
          "You will get your results in about ",
          showMinutes(in.eta.toMinutes.toInt atLeast 1),
          "."
        )
      ),
      div(cls := "tutor__waiting-games")(
        div(cls := "tutor__waiting-games__carousel")(waitGames.map(waitGame))
      )
    )

  private def waitGame(game: (Pov, PgnStr)) =
    div(
      cls            := "tutor__waiting-game lpv lpv--todo lpv--moves-false lpv--controls-false",
      st.data("pgn") := game._2.value,
      st.data("pov") := game._1.color.name
    )

  private def nbGames(user: User)(using Lang) = {
    val nb = lila.rating.PerfType.standardWithUltra.foldLeft(0) { (nb, pt) =>
      nb + user.perfs(pt).nb
    }
    p(s"Looks like you have ", strong(nb.atMost(10_000).localize), " rated games to look at, excellent!")
  }

  private def examinationMethod = p(
    "Using the best chess engine: ",
    views.html.plan.features.engineFullName,
    ", ",
    "and comparing your playstyle to thousands of other players with similar rating."
  )

  def insufficientGames(user: User)(using WebContext) =
    bits.layout(menu = emptyFrag, pageSmall = true)(
      cls := "tutor__insufficient box",
      boxTop(h1(bits.otherUser(user), "Lichess Tutor")),
      mascotSaysInsufficient
    )

  def mascotSaysInsufficient =
    bits.mascotSays(
      frag(
        strong("Not enough rated games to examine!"),
        br,
        "Please come back after you have played more chess."
      )
    )
