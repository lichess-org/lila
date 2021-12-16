package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object chatPanic {

  def apply(state: Option[org.joda.time.DateTime])(implicit ctx: Context) = {
    val title = "Chat Panic"
    views.html.base.layout(
      title = title,
      moreCss = cssTag("mod.misc")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("panic"),
        div(id := "chat-panic", cls := "page-menu__content box box-pad")(
          h1(title),
          p(
            "When Chat Panic is enabled, restrictions apply to public chats (tournament, simul) and PM",
            br,
            "Only players 24h old, and with 10 games played, can write messages."
          ),
          p(
            "Current state: ",
            state.map { s =>
              frag(
                goodTag(cls := "text", dataIcon := "E")(strong("ENABLED")),
                ". Expires ",
                momentFromNow(s)
              )
            } getOrElse badTag(cls := "text", dataIcon := "L")(strong("DISABLED"))
          ),
          div(cls := "forms")(
            if (state.isDefined)
              frag(
                postForm(action := s"${routes.Mod.chatPanicPost}?v=0")(
                  submitButton(cls := "button button-fat button-red text", dataIcon := "L")("Disable")
                ),
                postForm(action := s"${routes.Mod.chatPanicPost}?v=1")(
                  submitButton(cls := "button button-fat button-green text", dataIcon := "E")(
                    "Renew for two hours"
                  )
                )
              )
            else
              postForm(action := s"${routes.Mod.chatPanicPost}?v=1")(
                submitButton(cls := "button button-fat text", dataIcon := "E")("Enable")
              )
          )
        )
      )
    }
  }
}
