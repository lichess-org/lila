package views.html.simul

import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object show {

  def apply(
    sim: lidraughts.simul.Simul,
    socketVersion: lidraughts.socket.Socket.SocketVersion,
    data: play.api.libs.json.JsObject,
    chatOption: Option[lidraughts.chat.UserChat.Mine],
    stream: Option[lidraughts.streamer.Stream],
    team: Option[lidraughts.team.Team]
  )(implicit ctx: Context) = views.html.base.layout(
    moreCss = cssTag("simul.show"),
    title = sim.fullName,
    moreJs = frag(
      jsAt(s"compiled/lidraughts.simul${isProd ?? (".min")}.js"),
      embedJsUnsafe(s"""lidraughts.simul=${
        safeJsonValue(Json.obj(
          "data" -> data,
          "i18n" -> bits.jsI18n(),
          "socketVersion" -> socketVersion.value,
          "userId" -> ctx.userId,
          "chat" -> chatOption.map { c =>
            views.html.chat.json(
              c.chat,
              name = trans.chatRoom.txt(),
              timeout = c.timeout,
              public = true,
              resourceId = lidraughts.chat.Chat.ResourceId(s"simul/${c.chat.id}")
            )
          }
        ))
      }""")
    )
  ) {
      main(
        cls := List(
          "simul" -> true,
          "simul-created" -> sim.isCreated
        )
      )(
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
                      trans.casual()
                    )
                  )
                ),
                trans.simulHostExtraTime(),
                ": ",
                trans.nbMinutes.pluralSameTxt(sim.clock.hostExtraMinutes),
                br,
                trans.hostColorX(sim.color match {
                  case Some("white") => trans.white()
                  case Some("black") => trans.black()
                  case _ => trans.randomColor()
                }),
                sim.spotlight.flatMap(_.drawLimit).map { lim =>
                  frag(
                    br,
                    if (lim > 0) trans.drawOffersAfterX(lim)
                    else trans.drawOffersNotAllowed()
                  )
                },
                sim.targetPct.map { target =>
                  frag(
                    br,
                    trans.targetWinningPercentage(s"$target%")
                  )
                },
                sim.spotlight.flatMap(_.chatmode).filter(lidraughts.simul.Simul.ChatMode.Everyone!=).map { chatmode =>
                  frag(
                    br,
                    trans.chatAvailableForX(chatmode match {
                      case lidraughts.simul.Simul.ChatMode.Spectators => trans.spectatorsOnly()
                      case _ => trans.participantsOnly()
                    })
                  )
                },
                sim.allowed.filter(_.nonEmpty).map { allowed =>
                  frag(
                    br,
                    trans.simulParticipationLimited(allowed.size)
                  )
                }
              ),
              trans.by(userIdLink(sim.hostId.some)),
              " ",
              sim.startedAt.fold(sim.spotlight.map(s => absClientDateTime(s.startsAt)))(momentFromNow(_).some),
              team map { t =>
                frag(
                  br,
                  trans.mustBeInTeam(a(href := routes.Team.show(t.id))(t.name))
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
