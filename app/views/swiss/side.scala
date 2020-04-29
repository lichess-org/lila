package views
package html.swiss

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.swiss.Swiss

import controllers.routes

object side {

  private val separator = " • "

  def apply(s: Swiss, chat: Boolean)(implicit ctx: Context) = frag(
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
            s"${s.round}/${s.nbRounds} rounds"
          ),
          if (s.rated) trans.ratedTournament() else trans.casualTournament(),
          separator,
          "Swiss",
          (isGranted(_.ManageTournament) || (ctx.userId
            .has(s.createdBy) && !s.isFinished)) option frag(
            " ",
            a(href := routes.Tournament.edit(s.id.value), title := "Edit tournament")(iconTag("%"))
          )
        )
      ),
      s.description map { d =>
        st.section(cls := "description")(richText(d))
      },
      !s.isStarted option absClientDateTime(s.startsAt)
    ),
    chat option views.html.chat.frag
  )
}
