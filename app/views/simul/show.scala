package views.html.simul

import controllers.routes
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

object show {

  def apply(
      sim: lila.simul.Simul,
      socketVersion: lila.socket.Socket.SocketVersion,
      data: play.api.libs.json.JsObject,
      chatOption: Option[lila.chat.UserChat.Mine],
      stream: Option[lila.streamer.Stream],
      team: Option[lila.team.Team]
  )(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("simul.show"),
      title = sim.fullName,
      moreJs = frag(
        jsModule("simul"),
        embedJsUnsafeLoadThen(s"""LichessSimul.start(${safeJsonValue(
          Json.obj(
            "data"          -> data,
            "i18n"          -> bits.jsI18n(),
            "socketVersion" -> socketVersion.value,
            "userId"        -> ctx.userId,
            "chat" -> chatOption.map { c =>
              views.html.chat.json(
                c.chat,
                name = trans.chatRoom.txt(),
                timeout = c.timeout,
                public = true,
                resourceId = lila.chat.Chat.ResourceId(s"simul/${c.chat.id}"),
                localMod = ctx.userId has sim.hostId
              )
            }
          )
        )})""")
      )
    ) {
      main(cls := "simul")(
        st.aside(cls := "simul__side")(
          div(cls := "simul__meta")(
            div(cls := "game-infos")(
              div(cls := "header")(
                iconTag("f"),
                div(
                  span(cls := "clock")(sim.clock.config.show),
                  div(cls := "setup")(
                    sim.variants.map(_.name).mkString(", "),
                    " • ",
                    trans.casual(),
                    (isGranted(_.ManageSimul) || ctx.userId.has(sim.hostId)) && sim.isCreated option frag(
                      " • ",
                      a(href := routes.Simul.edit(sim.id), title := "Edit simul")(iconTag("%"))
                    )
                  )
                )
              ),
              trans.simulHostExtraTime(),
              ": ",
              pluralize("minute", sim.clock.hostExtraMinutes),
              br,
              trans.hostColorX(sim.color match {
                case Some("white") => trans.white()
                case Some("black") => trans.black()
                case _             => trans.randomColor()
              }),
              sim.position.flatMap(lila.tournament.Thematic.byFen) map { pos =>
                frag(
                  br,
                  a(targetBlank, href := pos.url)(strong(pos.eco), " ", pos.name),
                  " • ",
                  views.html.base.bits.fenAnalysisLink(pos.fen)
                )
              } orElse sim.position.map { fen =>
                frag(
                  br,
                  "Custom position • ",
                  views.html.base.bits.fenAnalysisLink(fen)
                )
              }
            ),
            trans.by(userIdLink(sim.hostId.some)),
            team map { t =>
              frag(
                br,
                trans.mustBeInTeam(a(href := routes.Team.show(t.id))(t.name))
              )
            },
            sim.estimatedStartAt map { d =>
              frag(
                br,
                absClientDateTime(d)
              )
            }
          ),
          stream.map { s =>
            views.html.streamer.bits.contextual(s.streamer.userId)
          },
          chatOption.isDefined option views.html.chat.frag
        ),
        div(cls := "simul__main box")(spinner)
      )
    }
}
