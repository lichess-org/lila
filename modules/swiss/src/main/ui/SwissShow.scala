package lila.swiss
package ui

import play.api.libs.json.*
import scalalib.paginator.Paginator

import lila.common.Json.given
import lila.common.String.html.markdownLinksOrRichText
import lila.core.config.NetDomain
import lila.core.team.LightTeam
import lila.gathering.Condition.WithVerdicts
import lila.gathering.ui.GatheringUi
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class SwissShow(helpers: Helpers, ui: SwissBitsUi, gathering: GatheringUi)(using NetDomain):
  import helpers.{ *, given }

  private def fullName(s: Swiss, t: LightTeam) = s"${s.name} by ${t.name}"

  def apply(
      s: Swiss,
      team: LightTeam,
      verdicts: WithVerdicts,
      data: play.api.libs.json.JsObject,
      chatOption: Option[(JsObject, Frag)],
      streamers: Frag,
      isLocalMod: Boolean
  )(using ctx: Context): Page =
    val isDirector       = ctx.is(s.createdBy)
    val hasScheduleInput = isDirector && s.settings.manualRounds && s.isNotFinished
    Page(fullName(s, team))
      .css("swiss.show")
      .css(hasScheduleInput.option("bits.flatpickr"))
      .i18n(_.study, _.swiss, _.team)
      .js(hasScheduleInput.option(Esm("bits.flatpickr")))
      .js(
        PageModule(
          "swiss",
          Json
            .obj(
              "data"        -> data,
              "userId"      -> ctx.userId,
              "chat"        -> chatOption.map(_._1),
              "showRatings" -> ctx.pref.showRatings
            )
            .add("schedule" -> hasScheduleInput)
        )
      )
      .graph(
        OpenGraph(
          title = s"${fullName(s, team)}: ${s.variant.name} ${s.clock.show} #${s.id}",
          url = s"$netBaseUrl${routes.Swiss.show(s.id).url}",
          description =
            s"${s.nbPlayers} players compete in the ${showEnglishDate(s.startsAt)} ${s.name} Swiss tournament " +
              s"organized by ${team.name}. " +
              s.winnerId.fold("Winner is not yet decided."): winnerId =>
                s"${titleNameOrId(winnerId)} takes the prize home!"
        )
      ):
        main(cls := "swiss")(
          st.aside(cls := "swiss__side")(
            side(s, verdicts, streamers, chatOption.map(_._2))
          ),
          div(cls := "swiss__main")(div(cls := "box"))
        )

  def round(s: Swiss, r: SwissRoundNumber, team: LightTeam, pairings: Paginator[SwissPairing])(using
      Context
  ) =
    Page(s"${fullName(s, team)} • Round $r/${s.round}")
      .css("swiss.show"):
        val pager = pagination(p => routes.Swiss.round(s.id, p).url, r.value, s.round.value, showPost = true)
        main(cls := "box swiss__round")(
          boxTop(
            h1(
              a(href := routes.Swiss.show(s.id))(s.name),
              s" • Round $r/${s.round}"
            )
          ),
          pager(cls := "pagination--top"),
          table(cls := "slist slist-pad")(
            pairings.currentPageResults.map: p =>
              tr(cls := "paginated")(
                td(a(href := routes.Round.watcher(p.gameId, Color.white), cls := "glpt")(s"#${p.gameId}")),
                td(userIdLink(p.white.some)),
                td(p.strResultOf(chess.White)),
                td(p.strResultOf(chess.Black)),
                td(userIdLink(p.black.some))
              )
          ),
          pager(cls := "pagination--bottom")
        )

  private val separator = " • "

  def side(s: Swiss, verdicts: WithVerdicts, streamers: Frag, chat: Option[Frag])(using
      ctx: Context
  ) =
    frag(
      div(cls := "swiss__meta")(
        st.section(dataIcon := s.perfType.icon.toString)(
          div(
            p(
              s.clock.show,
              separator,
              variantLink(s.variant, s.perfType, shortName = true),
              separator,
              if s.settings.rated then trans.site.ratedTournament() else trans.site.casualTournament()
            ),
            p(
              span(cls := "swiss__meta__round")(
                trans.swiss.nbRounds.plural(s.settings.nbRounds, s"${s.round}/${s.settings.nbRounds}")
              ),
              separator,
              a(href := routes.Swiss.home)(trans.swiss.swiss()),
              (Granter.opt(_.ManageTournament) || (ctx.is(s.createdBy) && s.isEnterable)).option(
                frag(
                  " ",
                  a(href := routes.Swiss.edit(s.id), title := "Edit tournament")(iconTag(Icon.Gear))
                )
              )
            ),
            ui.showInterval(s)
          )
        ),
        s.settings.description.map: d =>
          st.section(cls := "description")(markdownLinksOrRichText(d)),
        s.looksLikePrize.option(gathering.userPrizeDisclaimer(s.createdBy)),
        s.settings.position
          .flatMap(p => lila.gathering.Thematic.byFen(p.opening))
          .map: pos =>
            div(a(targetBlank, href := pos.url)(pos.name))
          .orElse(s.settings.position.map: fen =>
            div(
              trans.site.customPosition(),
              " • ",
              lila.ui.bits.fenAnalysisLink(fen)
            )),
        teamLink(s.teamId),
        gathering.verdicts(verdicts, s.perfType, s.isEnterable) | br,
        small(trans.site.by(userIdLink(s.createdBy.some))),
        br,
        absClientInstant(s.startsAt)
      ),
      streamers,
      chat
    )
