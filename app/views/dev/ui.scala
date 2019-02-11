package views.html.dev

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import play.api.data.Form

object ui {

  def apply(form: Form[_], captcha: lila.common.Captcha)(implicit ctx: Context) = views.html.base.layout(
    title = "UI test",
    moreCss = responsiveCssTag("palette"),
    responsive = true
  ) {
      main(cls := "ui-test box box-pad")(
        h1("H1 header title"),
        st.form(cls := "form3")(
          views.html.base.captcha(form, captcha)
        ),
        h2("H2 header title"),
        h3("H3 header title"),
        h4("H4 header title"),
        p(
          "<p> Random quote: ",
          lila.quote.Quote.one.text
        ),
        p(
          "<p> Random quotes: ",
          (1 to 5).map(_ => lila.quote.Quote.one.text).mkString(" ")
        ),
        div(cls := "palette")(
          List("background", "primary", "secondary", "accent", "brag", "error", "fancy", "font").map { c =>
            div(cls := s"color $c")(div(cls := "variants"))
          }
        )
      )
    }.toHtml
}
