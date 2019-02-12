package views
package html.tournament

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object side {

  private val separator = " • "

  def apply(
    tour: lidraughts.tournament.Tournament,
    verdicts: lidraughts.tournament.Condition.All.WithVerdicts,
    streamers: Set[lidraughts.user.User.ID],
    shieldOwner: Option[lidraughts.tournament.TournamentShield.OwnerId]
  )(implicit ctx: Context) = frag(
    div(cls := "side_box padded")(
      div(cls := "game_infos", dataIcon := tour.perfType.map(_.iconChar.toString))(
        div(cls := "header")(
          isGranted(_.TerminateTournament) option
            scalatags.Text.all.form(cls := "terminate", method := "post", action := routes.Tournament.terminate(tour.id), style := "float:right")(
              button(dataIcon := "j", cls := "submit text fbt confirm", `type` := "submit", title := "Terminates the tournament immediately")
            ),
          span(cls := "setup")(
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
          " ",
          a(cls := "blue help", href := routes.Tournament.help(tour.system.toString.toLowerCase.some), dataIcon := "")
        )
      ),
      tour.spotlight map { s =>
        div(cls := "game_infos spotlight")(
          lidraughts.common.String.html.markdownLinks(s.description),
          shieldOwner map { owner =>
            p(cls := "defender", dataIcon := "5")(
              "Defender:",
              userIdLink(owner.value.some)
            )
          }
        )
      },
      verdicts.relevant option div(dataIcon := "7", cls := List(
        "game_infos conditions" -> true,
        "accepted" -> (ctx.isAuth && verdicts.accepted),
        "refused" -> (ctx.isAuth && !verdicts.accepted)
      ))(
        (verdicts.list.size < 2) option p(trans.conditionOfEntry()),
        verdicts.list map { v =>
          p(cls := List(
            "condition text" -> true,
            "accepted" -> v.verdict.accepted,
            "refused" -> !v.verdict.accepted
          ))(v.condition.name(ctx.lang))
        }
      ),
      tour.noBerserk option div(cls := "text", dataIcon := "`")("No Berserk allowed"),
      !tour.isScheduled option frag(trans.by(usernameOrId(tour.createdBy)), br),
      !tour.isStarted option absClientDateTime(tour.startsAt),
      (!tour.position.initial) ?? frag(
        br, br,
        a(target := "_blank", href := tour.position.url)(
          strong(tour.position.eco),
          s" ${tour.position.name}"
        )
      )
    ),
    streamers.toList map { id =>
      a(href := routes.Streamer.show(id), cls := "context-streamer text side_box", dataIcon := "")(
        usernameOrId(id),
        " is streaming"
      )
    }
  )
}
