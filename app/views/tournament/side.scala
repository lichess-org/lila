package views
package html.tournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.tournament.{ TeamBattle, Tournament, TournamentShield }

import controllers.routes

object side {

  private val separator = " - "

  def apply(
      tour: Tournament,
      verdicts: lila.tournament.Condition.All.WithVerdicts,
      streamers: List[lila.user.User.ID],
      shieldOwner: Option[TournamentShield.OwnerId],
      chat: Boolean
  )(implicit ctx: Context) =
    frag(
      div(cls := "tour__meta")(
        st.section(dataIcon := tour.perfType.map(_.iconChar.toString))(
          div(
            p(
              tour.clock.show,
              separator,
              views.html.game.bits.variantLink(tour.variant, tour.perfType),
              tour.position.isDefined ?? s"$separator${trans.thematic.txt()}",
              separator,
              tour.durationString
            ),
            tour.mode.fold(trans.casualTournament, trans.ratedTournament)(),
            separator,
            "Arena",
            (isGranted(_.ManageTournament) || (ctx.userId
              .has(tour.createdBy) && !tour.isFinished)) option frag(
              " ",
              a(href := routes.Tournament.edit(tour.id), title := "Edit tournament")(iconTag("%"))
            )
          )
        ),
        tour.teamBattle map teamBattle(tour),
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
        tour.description map { d =>
          st.section(cls := "description")(richText(d))
        },
        tour.looksLikePrize option bits.userPrizeDisclaimer,
        verdicts.relevant option st.section(
          dataIcon := "7",
          cls := List(
            "conditions" -> true,
            "accepted"   -> (ctx.isAuth && verdicts.accepted),
            "refused"    -> (ctx.isAuth && !verdicts.accepted)
          )
        )(
          div(
            (verdicts.list.sizeIs < 2) option p(trans.conditionOfEntry()),
            verdicts.list map { v =>
              p(
                cls := List(
                  "condition text" -> true,
                  "accepted"       -> v.verdict.accepted,
                  "refused"        -> !v.verdict.accepted
                )
              )(v.condition match {
                case lila.tournament.Condition.TeamMember(teamId, teamName) =>
                  trans.mustBeInTeam(teamLink(teamId, teamName, withIcon = false))
                case c => c.name
              })
            }
          )
        ),
        tour.noBerserk option div(cls := "text", dataIcon := "`")(trans.arena.noBerserkAllowed()),
        tour.noStreak option div(cls := "text", dataIcon := "Q")(trans.arena.noArenaStreaks()),
        !tour.isScheduled option frag(trans.by(userIdLink(tour.createdBy.some)), br),
        (!tour.isStarted || (tour.isScheduled && tour.position.isDefined)) option absClientDateTime(
          tour.startsAt
        ),
        tour.startingPosition.map { pos =>
          p(
            frag(strong(pos.japanese), s" (${pos.english})"),
            separator,
            views.html.base.bits.sfenAnalysisLink(pos.sfen)
          )
        } orElse tour.position.map { sfen =>
          p(
            "Custom position",
            separator,
            views.html.base.bits.sfenAnalysisLink(sfen)
          )
        }
      ),
      streamers.nonEmpty option div(cls := "context-streamers")(
        streamers map views.html.streamer.bits.contextual
      ),
      chat option views.html.chat.frag
    )

  private def teamBattle(tour: Tournament)(battle: TeamBattle)(implicit ctx: Context) =
    st.section(cls := "team-battle")(
      p(cls := "team-battle__title text", dataIcon := "f")(
        s"Battle of ${battle.teams.size} teams and ${battle.nbLeaders} leaders",
        (ctx.userId.has(tour.createdBy) || isGranted(_.ManageTournament)) option
          a(href := routes.Tournament.teamBattleEdit(tour.id), title := "Edit team battle")(iconTag("%"))
      )
    )
}
