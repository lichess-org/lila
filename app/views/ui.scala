package views.html

import lila.app.templating.Environment.*

lazy val chat = lila.chat.ChatUi(helpers)

lazy val gathering = lila.gathering.ui.GatheringUi(helpers)(env.web.settings.prizeTournamentMakers.get)
