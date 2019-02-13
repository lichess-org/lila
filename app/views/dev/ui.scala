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
        h2("H2 header title"),
        h3("H3 header title"),
        h4("H4 header title"),
        div(cls := "buttons")(for {
          full <- List("", "empty")
          size <- List("thin", "", "fat")
          color <- List("", "green", "red", "metal")
        } yield div(cls := List(
          "button" -> true,
          s"button-$full" -> full.nonEmpty,
          s"button-$size" -> size.nonEmpty,
          s"button-$color" -> color.nonEmpty
        ))(s"button $full $size $color")),
        p("<p> Random quotes: ", (1 to 5).map(_ => lila.quote.Quote.one.text).mkString(" ")),
        List("shade", "dimmer", "dim", "", "clear", "clearer").map { v =>
          p(cls := s"font-$v")(s"<p $v> Random quote: ", lila.quote.Quote.one.text)
        },
        br,
        div(cls := "palette")(
          List("background", "primary", "secondary", "accent", "brag", "error", "fancy", "font").map { c =>
            div(cls := s"color $c")(div(cls := "variants"))
          }
        ),
        st.form(cls := "form3")(
          views.html.base.captcha(form, captcha)
        ),
        st.tag("signal")(cls := "q4")(
          i, i, i(cls := "off"), i(cls := "off")
        )
      )
    }
}
