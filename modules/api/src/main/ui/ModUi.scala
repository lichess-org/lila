package lila.api
package ui

import lila.ui.*
import lila.shutup.PublicLine
import lila.core.i18n.Translate
import lila.core.shutup.PublicSource

object ModUi:
  def renderPublicLineSource(line: PublicLine)(using Translate) = line.from.map:
    case PublicSource.Tournament(id) => lila.tournament.ui.tournamentLink(id)
    case PublicSource.Simul(id)      => views.simul.ui.link(id)
    case PublicSource.Team(id)       => teamLink(id)
    case PublicSource.Watcher(id) =>
      a(href := routes.Round.watcher(id, Color.white))("Game #", id)
    case PublicSource.Study(id) => a(href := routes.Study.show(id))("Study #", id)
    case PublicSource.Swiss(id) => views.swiss.ui.link(id)
    case PublicSource.Forum(id) => a(href := routes.ForumPost.redirect(id))("Forum #", id)
    case PublicSource.Ublog(id) => a(href := routes.Ublog.redirect(id))("User blog #", id)
