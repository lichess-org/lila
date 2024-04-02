package views
package html.tournament

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.markdownLinksOrRichText
import lila.tournament.{ TeamBattle, Tournament }

object side:

  private val separator = " • "

  def apply(
      tour: Tournament,
      verdicts: lila.gathering.Condition.WithVerdicts,
      streamers: List[UserId],
      shieldOwner: Option[UserId],
      chat: Boolean
  )(using ctx: Context) =
    frag(
      div(cls := "tour__meta")(
        st.section(dataIcon := tour.perfType.icon.toString)(
          div(
            p(
              tour.clock.show,
              separator,
              views.html.game.bits.variantLink(
                tour.variant,
                tour.perfType,
                shortName = true
              ),
              tour.position.isDefined.so(s"$separator${trans.site.thematic.txt()}"),
              separator,
              tour.durationString
            ),
            if tour.mode.rated then trans.site.ratedTournament() else trans.site.casualTournament(),
            separator,
            trans.arena.arena(),
            (isGranted(_.ManageTournament) || (ctx.is(tour.createdBy) && tour.isEnterable)).option(
              frag(
                " ",
                a(href := routes.Tournament.edit(tour.id), title := trans.arena.editTournament.txt())(
                  iconTag(licon.Gear)
                )
              )
            )
          )
        ),
        tour.teamBattle.map(teamBattle(tour)),
        variantTeamLinks
          .get(tour.variant.key)
          .filter { (team, _) =>
            tour.createdBy.is(lila.user.User.lichessId) || tour.conditions.teamMember
              .exists(_.teamId == team.id)
          }
          .map { (team, link) =>
            st.section(
              if isMyTeamSync(team.id) then frag(trans.team.team(), " ", link)
              else trans.team.joinLichessVariantTeam(link)
            )
          },
        tour.description.map: d =>
          st.section(cls := "description")(
            shieldOwner.map: owner =>
              p(cls := "defender", dataIcon := licon.Shield)(trans.arena.defender(), userIdLink(owner.some)),
            markdownLinksOrRichText(d)
          ),
        tour.looksLikePrize.option(bits.userPrizeDisclaimer(tour.createdBy)),
        views.html.gathering.verdicts(verdicts, tour.perfType, tour.isEnterable),
        tour.noBerserk.option(div(cls := "text", dataIcon := licon.Berserk)(trans.arena.noBerserkAllowed())),
        tour.noStreak.option(div(cls := "text", dataIcon := licon.Fire)(trans.arena.noArenaStreaks())),
        (!tour.isScheduled).option(frag(small(trans.site.by(userIdLink(tour.createdBy.some))), br)),
        (!tour.isStarted || (tour.isScheduled && tour.position.isDefined)).option(
          absClientInstant(
            tour.startsAt
          )
        ),
        tour.startingPosition
          .map { pos =>
            p(a(href := pos.url)(pos.name))
          }
          .orElse(tour.position.map { fen =>
            p(
              trans.site.customPosition(),
              separator,
              views.html.base.bits.fenAnalysisLink(fen.into(chess.format.Fen.Full))
            )
          })
      ),
      views.html.streamer.bits.contextual(streamers),
      chat.option(views.html.chat.frag)
    )

  private def teamBattle(tour: Tournament)(battle: TeamBattle)(using ctx: Context) =
    st.section(cls := "team-battle", dataIcon := licon.Group):
      div(
        p(trans.team.battleOfNbTeams.pluralSameTxt(battle.teams.size)),
        trans.team.nbLeadersPerTeam.pluralSameTxt(battle.nbLeaders),
        (ctx.is(tour.createdBy) || isGranted(_.ManageTournament)).option(
          frag(
            " ",
            a(href := routes.Tournament.teamBattleEdit(tour.id), title := trans.arena.editTeamBattle.txt()):
              iconTag(licon.Gear)
          )
        )
      )
