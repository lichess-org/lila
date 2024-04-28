package views.swiss

import lila.app.templating.Environment.{ *, given }

import lila.common.String.html.markdownLinksOrRichText
import lila.gathering.Condition
import lila.gathering.Condition.WithVerdicts
import lila.swiss.Swiss

object side:

  private val separator = " • "

  def apply(
      s: Swiss,
      verdicts: WithVerdicts,
      streamers: List[UserId],
      chat: Boolean
  )(using ctx: Context) =
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
              (isGranted(_.ManageTournament) || (ctx.is(s.createdBy) && s.isEnterable)).option(
                frag(
                  " ",
                  a(href := routes.Swiss.edit(s.id), title := "Edit tournament")(iconTag(Icon.Gear))
                )
              )
            ),
            bits.showInterval(s)
          )
        ),
        s.settings.description.map: d =>
          st.section(cls := "description")(markdownLinksOrRichText(d)),
        s.looksLikePrize.option(views.gathering.userPrizeDisclaimer(s.createdBy)),
        s.settings.position
          .flatMap(p => lila.tournament.Thematic.byFen(p.opening))
          .map { pos =>
            div(a(targetBlank, href := pos.url)(pos.name))
          }
          .orElse(s.settings.position.map: fen =>
            div(
              trans.site.customPosition(),
              " • ",
              lila.ui.bits.fenAnalysisLink(fen)
            )),
        teamLink(s.teamId),
        views.gathering.verdicts(verdicts, s.perfType, s.isEnterable) | br,
        small(trans.site.by(userIdLink(s.createdBy.some))),
        br,
        absClientInstant(s.startsAt)
      ),
      views.streamer.bits.contextual(streamers),
      chat.option(views.chat.frag)
    )
