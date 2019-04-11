package views.html.site

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  def getDraughtsnet()(implicit ctx: Context) =
    views.html.base.layout(
      title = "Draughtsnet API key request",
      csp = defaultCsp.withGoogleForm.some
    ) {
      main(
        iframe(
          //src := "https://docs.google.com/forms/d/e/1FAIpQLSeSAp51tSaW9JlPGVX0o8dcScAuxGMhNOL9eEUIfARGzpITmA/viewform?embedded=true",
          style := "width:100%;height:1400px",
          st.frameBorder := 0
        )(spinner)
      )
    }
}
