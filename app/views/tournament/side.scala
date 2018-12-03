package views
package html.tournament

import scalatags.Text.all._

import lila.api.Context
import lila.app.templating.Environment._
import lila.i18n.{ I18nKeys => trans }

import controllers.routes

object side {

  private val separator = " • "

  def apply(
    tour: lila.tournament.Tournament,
    verdicts: lila.tournament.Condition.All.WithVerdicts,
    streamers: Set[lila.user.User.ID],
    shieldOwner: Option[lila.tournament.TournamentShield.OwnerId]
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
                if (tour.variant == chess.variant.KingOfTheHill) tour.variant.shortName else tour.variant.name,
                cssClass = "hint--top"
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
          lila.common.String.html.markdownLinks(s.description),
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
      if (tour.createdBy == "lichess") trans.tournamentOfficial()
      else trans.by(usernameOrId(tour.createdBy)),
      !tour.isStarted option frag(
        br,
        absClientDateTime(tour.startsAt)
      ),
      (!tour.position.initial) ?? List(
        br, br,
        a(target := "_blank", href := tour.position.url)(
          strong(tour.position.eco),
          s" ${tour.position.name}"
        )
      ),
      tour.winnerId ?? { userId =>
        List[Modifier](
          br, br,
          trans.winner(),
          ": ",
          userIdLink(userId.some)
        )
      }
    ),
    streamers.toList map { id =>
      a(href := routes.Streamer.show(id), cls := "context-streamer text side_box", dataIcon := "")(
        usernameOrId(id),
        " is streaming"
      )
    }
  )
}
