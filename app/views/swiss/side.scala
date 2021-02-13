package views
package html.swiss

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.markdownLinksOrRichText
import lila.swiss.Swiss

import controllers.routes

object side {

  private val separator = " • "

  def apply(s: Swiss, chat: Boolean)(implicit ctx: Context) =
    frag(
      div(cls := "swiss__meta")(
        st.section(dataIcon := s.perfType.map(_.iconChar.toString))(
          div(
            p(
              s.clock.show,
              separator,
              if (s.variant.exotic) {
                views.html.game.bits.variantLink(
                  s.variant,
                  if (s.variant == chess.variant.KingOfTheHill) s.variant.shortName
                  else s.variant.name
                )
              } else s.perfType.map(_.trans),
              separator,
              if (s.settings.rated) trans.ratedTournament() else trans.casualTournament()
            ),
            p(
              span(cls := "swiss__meta__round")(s"${s.round}/${s.settings.nbRounds}"),
              " rounds",
              separator,
              a(href := routes.Swiss.home())("Swiss"),
              (isGranted(_.ManageTournament) || (ctx.userId.has(s.createdBy) && !s.isFinished)) option frag(
                " ",
                a(href := routes.Swiss.edit(s.id.value), title := "Edit tournament")(iconTag("%"))
              )
            ),
            bits.showInterval(s)
          )
        ),
        s.settings.description map { d =>
          st.section(cls := "description")(markdownLinksOrRichText(d))
        },
        s.looksLikePrize option views.html.tournament.bits.userPrizeDisclaimer(s.createdBy),
        teamLink(s.teamId),
        separator,
        absClientDateTime(s.startsAt)
      ),
      chat option views.html.chat.frag
    )
}
