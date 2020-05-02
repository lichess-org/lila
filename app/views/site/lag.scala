package views.html.site

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object lag {

  import trans.lag._

  def apply()(implicit ctx: Context) =
    help.layout(
      title = "Is Lichess lagging?",
      active = "lag",
      moreCss = cssTag("lag"),
      moreJs = frag(
        highchartsLatestTag,
        highchartsMoreTag,
        jsTag("lag.js")
      )
    ) {
      main(cls := "box box-pad lag")(
        h1(
          isLichessLagging(),
          span(cls := "answer short")(
            span(cls := "waiting")(measurementInProgressThreeDot()),
            span(cls := "nope-nope none")(strong(trans.no(), "."), " ", andYourNetworkIsGood()),
            span(cls := "nope-yep none")(strong(trans.no(), "."), " ", andYourNetworkIsBad()),
            span(cls := "yep none")(strong(trans.yes(), "."), " ", itWillBeFixedSoon())
          )
        ),
        div(cls := "answer long")(
          andNowTheLongAnswerLagComposedOfTwoValues()
        ),
        div(cls := "sections")(
          st.section(cls := "server")(
            h2(lichessServerLatency()),
            div(cls := "meter"),
            p(
              lichessServerLatencyExplanation(
                strong(sameForEverybody())
              )
            )
          ),
          st.section(cls := "network")(
            h2(networkBetweenLichessAndYou()),
            div(cls := "meter"),
            p(
              networkBetweenLichessAndYouExplanation(
                strong(distanceToLichessFrance()),
                strong(qualityOfYourInternetConnection())
              )
            )
          )
        ),
        div(cls := "last-word")(
          p(youCanFindTheseValuesAtAnyTimeByClickingOnYourUsername()),
          h2(lagCompensation()),
          p(
            lagCompensationExplanation(
              strong(notAHandicap())
            )
          )
        )
      )
    }
}
