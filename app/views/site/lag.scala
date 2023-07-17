package views.html.site

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*

object lag:

  import trans.lag.*

  def apply()(using PageContext) =
    page.layout(
      title = "Is Lichess lagging?",
      active = "lag",
      moreCss = cssTag("lag"),
      moreJs = frag(
        highchartsLatestTag,
        highchartsMoreTag,
        iifeModule("javascripts/lag.js")
      )
    ):
      div(cls := "box box-pad lag")(
        h1(cls := "box__top")(
          isLichessLagging(),
          span(cls := "answer short")(
            span(cls := "waiting")(measurementInProgressThreeDot()),
            span(cls := "nope-nope none")(noAndYourNetworkIsGood()),
            span(cls := "nope-yep none")(noAndYourNetworkIsBad()),
            span(cls := "yep none")(yesItWillBeFixedSoon())
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
              lichessServerLatencyExplanation()
            )
          ),
          st.section(cls := "network")(
            h2(networkBetweenLichessAndYou()),
            div(cls := "meter"),
            p(
              networkBetweenLichessAndYouExplanation()
            )
          )
        ),
        div(cls := "last-word")(
          p(youCanFindTheseValuesAtAnyTimeByClickingOnYourUsername()),
          h2(lagCompensation()),
          p(
            lagCompensationExplanation()
          )
        )
      )
