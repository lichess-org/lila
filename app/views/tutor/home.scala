package views.tutor

import play.api.i18n.Lang
import chess.format.pgn.PgnStr

import lila.app.templating.Environment.{ *, given }
import lila.tutor.TutorQueue
import lila.core.perf.UserWithPerfs

object home:

  lazy val ui = lila.tutor.ui.TutorHome(helpers, bits, perf.ui)

  def apply(full: lila.tutor.TutorFullReport.Available, user: User)(using PageContext) =
    layout(menu = bits.menu(full, user, none))(cls := "tutor__home box", ui.home(full, user))

object empty:

  def start(user: User)(using PageContext) =
    layout(menu = emptyFrag, pageSmall = true)(
      cls := "tutor__empty box",
      home.ui.empty.start(user)
    )

  def queued(in: TutorQueue.InQueue, user: UserWithPerfs, waitGames: List[(Pov, PgnStr)])(using
      PageContext
  ) =
    layout(menu = emptyFrag, title = "Lichess Tutor - Examining games...", pageSmall = true)(
      cls := "tutor__empty tutor__queued box",
      home.ui.empty.queued(in, user, waitGames)
    )

  def insufficientGames(user: User)(using PageContext) =
    layout(menu = emptyFrag, pageSmall = true)(
      cls := "tutor__insufficient box",
      boxTop(h1(bits.otherUser(user), "Lichess Tutor")),
      home.ui.empty.mascotSaysInsufficient
    )
