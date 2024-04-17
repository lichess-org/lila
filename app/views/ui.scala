package views.html

import com.softwaremill.macwire.*

export lila.app.templating.Environment.*

lazy val chat = wire[lila.chat.ChatUi]
