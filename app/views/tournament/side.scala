package views
package html.tournament

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.richText

import controllers.routes

object side {

  private val separator = " • "

  def apply(
    tour: lidraughts.tournament.Tournament,
    verdicts: lidraughts.tournament.Condition.All.WithVerdicts,
    streamers: Set[lidraughts.user.User.ID],
    shieldOwner: Option[lidraughts.tournament.TournamentShield.OwnerId],
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
                tour.variant.name
              )
            } else tour.perfType.map(_.name),
            (!tour.position.initial) ?? s"• ${trans.thematic.txt()}",
            separator,
            tour.durationString
          ),
          tour.mode.fold(trans.casualTournament, trans.ratedTournament)(),
          separator,
          systemName(tour.system).capitalize,
          (isGranted(_.ManageTournament) || (ctx.userId.has(tour.createdBy) && tour.isCreated)) option frag(
            " ",
            a(href := routes.Tournament.edit(tour.id), title := trans.editTournament.txt(), style := "float:right")(iconTag("%"))
          )
        )
      ),
      tour.spotlight map { s =>
        st.section(
          lidraughts.common.String.html.markdownLinks(s.description),
          shieldOwner map { owner =>
            p(cls := "defender", dataIcon := "5")(
              "Defender:",
              userIdLink(owner.value.some)
            )
          }
        )
      },
      tour.description map { d =>
        st.section(cls := "description")(richText(d))
      },
      verdicts.relevant option st.section(dataIcon := "7", cls := List(
        "conditions" -> true,
        "accepted" -> (ctx.isAuth && verdicts.accepted),
        "refused" -> (ctx.isAuth && !verdicts.accepted)
      ))(div(
        (verdicts.list.size < 2) option p(trans.conditionOfEntry()),
        verdicts.list map { v =>
          p(cls := List(
            "condition text" -> true,
            "accepted" -> v.verdict.accepted,
            "refused" -> !v.verdict.accepted
          ))(v.condition match {
            case lidraughts.tournament.Condition.TeamMember(teamId, teamName) =>
              trans.mustBeInTeam(teamLink(teamId, lidraughts.common.String.html.escapeHtml(teamName), withIcon = false))
            case c => c.name(ctx.lang)
          })
        }
      )),
      tour.noBerserk option div(cls := "text", dataIcon := "`")(trans.noBerserkAllowed()),
      !tour.isScheduled option frag(trans.by(userIdLink(tour.createdBy.some)), br),
      (!tour.isStarted || (tour.isScheduled && !tour.position.initial)) option absClientDateTime(tour.startsAt),
      !tour.position.initial option p(
        a(target := "_blank", href := tour.position.url)(
          strong(tour.position.eco), " ", tour.position.name
        )
      )
    ),
    streamers.toList map views.html.streamer.bits.contextual,
    chat option views.html.chat.frag
  )
}
