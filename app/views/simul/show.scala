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
    stream: Option[lidraughts.streamer.Stream]
  )(implicit ctx: Context) = bits.layout(
    title = sim.fullName,
    side = Some(frag(
      div(cls := "side_box padded")(
        div(cls := "game_infos")(
          div(cls := List(
            "variant_icons" -> true,
            "rich" -> sim.variantRich
          ))(sim.perfTypes.map { pt => span(dataIcon := pt.iconChar) }),
          span(cls := "clock")(sim.clock.config.show),
          br,
          div(cls := "setup")(
            sim.variants.map(_.name).mkString(", "),
            " • ",
            trans.casual()
          ),
          trans.simulHostExtraTime(),
          ": ",
          pluralize("minute", sim.clock.hostExtraMinutes),
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
          (sim.chatmode.isDefined && !sim.chatmode.contains(lidraughts.simul.Simul.ChatMode.Everyone)) option {
            frag(
              br,
              trans.chatAvailableForX(sim.chatmode match {
                case Some(lidraughts.simul.Simul.ChatMode.Spectators) => trans.spectatorsOnly()
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
        trans.by(usernameOrId(sim.hostId)),
        " ",
        sim.spotlight.fold(momentFromNow(sim.createdAt)) { s => absClientDateTime(s.startsAt) }
      ),
      stream.map { s =>
        a(
          href := routes.Streamer.show(s.streamer.userId),
          cls := "context-streamer text side_box",
          dataIcon := ""
        )(usernameOrId(s.streamer.userId), " is streaming")
      }
    )),
    underchat = Some(div(
      cls := "watchers none",
      aria.live := "off",
      aria.relevant := "additions removals text"
    )(span(cls := "list inline_userlist"))),
    chat = views.html.chat.frag.some,
    moreJs = frag(
      jsAt(s"compiled/lidraughts.simul${isProd ?? (".min")}.js"),
      embedJs(s"""lidraughts.simul={
data:${safeJsonValue(data)},
i18n:${bits.jsI18n()},
socketVersion:${socketVersion.value},
userId: $jsUserIdString,
chat: ${chatOption.fold("null")(c => safeJsonValue(views.html.chat.json(c.chat, name = trans.chatRoom.txt(), timeout = c.timeout, public = true)))}}""")
    ),
    moreCss = cssTags(List(
      "chat.css" -> true,
      "quote.css" -> sim.isCreated
    ))
  ) {
      div(id := "simul")
    }
}
