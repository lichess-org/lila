package lila.tournament
package ui

import play.api.data.Form

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.team.LightTeam
import lila.common.String.html.markdownLinksOrRichText
import lila.core.config.NetDomain
import lila.gathering.ui.GatheringUi

final class TournamentShow(helpers: Helpers, gathering: GatheringUi)(
    variantTeamLinks: Map[chess.variant.Variant.LilaKey, (LightTeam, Frag)]
):
  import helpers.{ *, given }

  def show(
      tour: Tournament,
      verdicts: lila.gathering.Condition.WithVerdicts,
      shieldOwner: Option[UserId],
      chat: Option[Frag],
      streamers: Frag
  )(using ctx: Context)(using NetDomain) =
    val extraCls = tour.schedule.so: sched =>
      s" tour-sched tour-sched-${sched.freq.name} tour-speed-${sched.speed.name} tour-variant-${sched.variant.key} tour-id-${tour.id}"
    main(cls := s"tour$extraCls")(
      st.aside(cls := "tour__side"):
        side(tour, verdicts, shieldOwner, chat, streamers)
      ,
      div(cls := "tour__main")(div(cls := "box")),
      tour.isCreated.option(div(cls := "tour__faq"):
        faq(tour.mode.rated.some, tour.isPrivate.option(tour.id))
      )
    )

  object side:

    private val separator = " • "

    def apply(
        tour: Tournament,
        verdicts: lila.gathering.Condition.WithVerdicts,
        shieldOwner: Option[UserId],
        chat: Option[Frag],
        streamers: Frag
    )(using ctx: Context)(using NetDomain) =
      frag(
        div(cls := "tour__meta")(
          st.section(dataIcon := tour.perfType.icon.toString)(
            div(
              p(
                tour.clock.show,
                separator,
                variantLink(tour.variant, tour.perfType, shortName = true),
                tour.position.isDefined.so(s"$separator${trans.site.thematic.txt()}"),
                separator,
                tour.durationString
              ),
              if tour.mode.rated then trans.site.ratedTournament() else trans.site.casualTournament(),
              separator,
              trans.arena.arena(),
              (Granter.opt(_.ManageTournament) || (ctx.is(tour.createdBy) && tour.isEnterable)).option(
                frag(
                  " ",
                  a(href := routes.Tournament.edit(tour.id), title := trans.arena.editTournament.txt())(
                    iconTag(Icon.Gear)
                  )
                )
              )
            )
          ),
          tour.teamBattle.map(teamBattle(tour)),
          variantTeamLinks
            .get(tour.variant.key)
            .filter: (team, _) =>
              tour.createdBy.is(UserId.lichess) || tour.conditions.teamMember
                .exists(_.teamId == team.id)
            .map: (team, link) =>
              st.section(
                if isMyTeamSync(team.id) then frag(trans.team.team(), " ", link)
                else trans.team.joinLichessVariantTeam(link)
              ),
          tour.description.map: d =>
            st.section(cls := "description")(
              shieldOwner.map: owner =>
                p(cls := "defender", dataIcon := Icon.Shield)(trans.arena.defender(), userIdLink(owner.some)),
              markdownLinksOrRichText(d)
            ),
          tour.looksLikePrize.option(gathering.userPrizeDisclaimer(tour.createdBy)),
          gathering.verdicts(verdicts, tour.perfType, tour.isEnterable),
          tour.noBerserk.option(div(cls := "text", dataIcon := Icon.Berserk)(trans.arena.noBerserkAllowed())),
          tour.noStreak.option(div(cls := "text", dataIcon := Icon.Fire)(trans.arena.noArenaStreaks())),
          (!tour.isScheduled).option(frag(small(trans.site.by(userIdLink(tour.createdBy.some))), br)),
          (!tour.isStarted || (tour.isScheduled && tour.position.isDefined)).option(
            absClientInstant(
              tour.startsAt
            )
          ),
          tour.startingPosition
            .map: pos =>
              p(a(href := pos.url)(pos.name))
            .orElse(tour.position.map { fen =>
              p(
                trans.site.customPosition(),
                separator,
                lila.ui.bits.fenAnalysisLink(fen.into(chess.format.Fen.Full))
              )
            })
        ),
        streamers,
        chat
      )

    private def teamBattle(tour: Tournament)(battle: TeamBattle)(using ctx: Context) =
      st.section(cls := "team-battle", dataIcon := Icon.Group):
        div(
          p(trans.team.battleOfNbTeams.pluralSameTxt(battle.teams.size)),
          trans.team.nbLeadersPerTeam.pluralSameTxt(battle.nbLeaders),
          (ctx.is(tour.createdBy) || Granter.opt(_.ManageTournament)).option(
            frag(
              " ",
              a(href := routes.Tournament.teamBattleEdit(tour.id), title := trans.arena.editTeamBattle.txt()):
                iconTag(Icon.Gear)
            )
          )
        )
  end side

  object faq:

    import trans.{ arena as tra }

    def page(using Context) =
      main(cls := "page-small box box-pad page")(
        boxTop(
          h1(
            a(href := routes.Tournament.home, dataIcon := Icon.LessThan, cls := "text"),
            trans.site.tournamentFAQ()
          )
        ),
        div(cls := "body")(apply())
      )

    def apply(rated: Option[Boolean] = None, privateId: Option[String] = None)(using Context) =
      frag(
        privateId.map: id =>
          frag(
            h2(trans.arena.thisIsPrivate()),
            p(trans.arena.shareUrl(s"$netBaseUrl${routes.Tournament.show(id)}")) // XXX
          ),
        p(trans.arena.willBeNotified()),
        h2(trans.arena.isItRated()),
        rated match
          case Some(true)  => p(trans.arena.isRated())
          case Some(false) => p(trans.arena.isNotRated())
          case None        => p(trans.arena.someRated())
        ,
        h2(tra.howAreScoresCalculated()),
        p(tra.howAreScoresCalculatedAnswer()),
        h2(tra.berserk()),
        p(tra.berserkAnswer()),
        h2(tra.howIsTheWinnerDecided()),
        p(tra.howIsTheWinnerDecidedAnswer()),
        h2(tra.howDoesPairingWork()),
        p(tra.howDoesPairingWorkAnswer()),
        h2(tra.howDoesItEnd()),
        p(tra.howDoesItEndAnswer()),
        h2(tra.otherRules()),
        p(tra.thereIsACountdown()),
        p(tra.drawingWithinNbMoves.pluralSame(10)),
        p(tra.drawStreakStandard(30)),
        p(tra.drawStreakVariants()),
        table(cls := "slist slist-pad")(
          thead(
            tr(
              th(tra.variant()),
              th(tra.minimumGameLength())
            )
          ),
          tbody(
            tr(
              td(trans.site.standard(), ", Chess960, Horde"),
              td(30)
            ),
            tr(
              td("Antichess, Crazyhouse, King of the Hill"),
              td(20)
            ),
            tr(
              td("Three check, Atomic, Racing Kings"),
              td(10)
            )
          )
        )
      )
  end faq
