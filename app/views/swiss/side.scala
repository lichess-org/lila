package views
package html.swiss

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.markdownLinksOrRichText
import lila.swiss.Swiss
import lila.gathering.Condition
import lila.gathering.Condition.WithVerdicts

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
              views.html.game.bits.variantLink(s.variant, s.perfType, shortName = true),
              separator,
              if s.settings.rated then trans.ratedTournament() else trans.casualTournament()
            ),
            p(
              span(cls := "swiss__meta__round")(
                trans.swiss.nbRounds.plural(s.settings.nbRounds, s"${s.round}/${s.settings.nbRounds}")
              ),
              separator,
              a(href := routes.Swiss.home)("Swiss"),
              (isGranted(_.ManageTournament) || (ctx.is(s.createdBy) && !s.isFinished)) option frag(
                " ",
                a(href := routes.Swiss.edit(s.id), title := "Edit tournament")(iconTag(licon.Gear))
              )
            ),
            bits.showInterval(s)
          )
        ),
        s.settings.description.map: d =>
          st.section(cls := "description")(markdownLinksOrRichText(d)),
        s.looksLikePrize option views.html.tournament.bits.userPrizeDisclaimer(s.createdBy),
        s.settings.position.flatMap(p => lila.tournament.Thematic.byFen(p.opening)) map { pos =>
          div(a(targetBlank, href := pos.url)(pos.name))
        } orElse s.settings.position.map: fen =>
          div(
            "Custom position • ",
            views.html.base.bits.fenAnalysisLink(fen)
          ),
        teamLink(s.teamId),
        views.html.gathering.verdicts(verdicts, s.perfType) | br,
        small(trans.by(userIdLink(s.createdBy.some))),
        br,
        absClientInstant(s.startsAt)
      ),
      views.html.streamer.bits.contextual(streamers),
      chat option views.html.chat.frag
    )
