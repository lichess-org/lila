package views
package html.tournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object side {

  private val separator = " • "

  def apply(
    tour: lila.tournament.Tournament,
    verdicts: lila.tournament.Condition.All.WithVerdicts,
    streamers: Set[lila.user.User.ID],
    shieldOwner: Option[lila.tournament.TournamentShield.OwnerId],
    chat: Boolean
  )(implicit ctx: Context) = frag(
    div(cls := "tour__meta")(
      st.section(dataIcon := tour.perfType.map(_.iconChar.toString))(
        div(
          p(
            tour.clock.show,
            separator,
            if (tour.variant.exotic) {
              views.html.game.bits.variantLink(
                tour.variant,
                if (tour.variant == chess.variant.KingOfTheHill) tour.variant.shortName else tour.variant.name
              )
            } else tour.perfType.map(_.name),
            (!tour.position.initial) ?? s"• ${trans.thematic.txt()}",
            separator,
            tour.durationString
          ),
          tour.mode.fold(trans.casualTournament, trans.ratedTournament).frag(),
          separator,
          systemName(tour.system).capitalize,
          isGranted(_.TerminateTournament) option
            scalatags.Text.all.form(cls := "terminate", method := "post", action := routes.Tournament.terminate(tour.id))(
              button(dataIcon := "j", cls := "fbt fbt-red confirm", `type` := "submit", title := "Terminates the tournament immediately")
            )
        )
      ),
      tour.spotlight map { s =>
        st.section(
          lila.common.String.html.markdownLinks(s.description),
          shieldOwner map { owner =>
            p(cls := "defender", dataIcon := "5")(
              "Defender:",
              userIdLink(owner.value.some)
            )
          }
        )
      },
      verdicts.relevant option st.section(dataIcon := "7", cls := List(
        "conditions" -> true,
        "accepted" -> (ctx.isAuth && verdicts.accepted),
        "refused" -> (ctx.isAuth && !verdicts.accepted)
      ))(div(
        (verdicts.list.size < 2) option p(trans.conditionOfEntry.frag()),
        verdicts.list map { v =>
          p(cls := List(
            "condition text" -> true,
            "accepted" -> v.verdict.accepted,
            "refused" -> !v.verdict.accepted
          ))(v.condition.name(ctx.lang))
        }
      )),
      tour.noBerserk option div(cls := "text", dataIcon := "`")("No Berserk allowed"),
      !tour.isScheduled option frag(trans.by.frag(usernameOrId(tour.createdBy)), br),
      (!tour.isStarted || (tour.isScheduled && !tour.position.initial)) option absClientDateTime(tour.startsAt),
      !tour.position.initial option p(
        a(target := "_blank", href := tour.position.url)(
          strong(tour.position.eco), " ", tour.position.name
        )
      )
    ),
    streamers.toList map { id =>
      a(href := routes.Streamer.show(id), cls := "context-streamer text side_box", dataIcon := "")(
        usernameOrId(id),
        " is streaming"
      )
    },
    chat option views.html.chat.frag
  )
}
