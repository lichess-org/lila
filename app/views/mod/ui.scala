package views.mod

import lila.app.UiEnv.{ *, given }
import lila.mod.ui.*
import lila.shutup.Analyser.highlightBad
import lila.core.shutup.PublicSource

lazy val ui = ModUi(helpers)
lazy val userTable = ModUserTableUi(helpers, ui)
lazy val user = ModUserUi(helpers, ui)
lazy val gamify = GamifyUi(helpers, ui)
lazy val publicChat = PublicChatUi(helpers, ui)(highlightBad)
lazy val commUi = ModCommUi(helpers)(highlightBad)
lazy val inquiryUi = ModInquiryUi(helpers)(publicLineSource, env.mod.presets.getPmPresets, highlightBad)

val timeline = lila.api.ui.ModTimelineUi(helpers)(publicLineSource = publicLineSource)

private def publicLineSource(source: lila.core.shutup.PublicSource)(using Translate): Tag = source match
  case PublicSource.Tournament(id) => views.tournament.ui.tournamentLink(id)
  case PublicSource.Simul(id) => views.simul.ui.link(id)
  case PublicSource.Team(id) => teamLink(id)
  case PublicSource.Watcher(id) =>
    a(href := routes.Round.watcher(id, Color.white))("Game #", id)
  case PublicSource.Study(id) => a(href := routes.Study.show(id))("Study #", id)
  case PublicSource.Swiss(id) => views.swiss.ui.link(id)
  case PublicSource.Forum(id) => a(href := routes.ForumPost.redirect(id))("Forum #", id)
  case PublicSource.Ublog(id) => a(href := routes.Ublog.redirect(id))("User blog #", id)
  case PublicSource.Relay(id) => a(href := routes.RelayRound.show("-", "-", id))("Broadcast #", id)

def permissions(u: User)(using Context, Me) =
  ui.permissions(u, lila.security.Permission.categorized)
