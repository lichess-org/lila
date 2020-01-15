package views.html.user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object download {

  def apply(user: lila.user.User)(implicit ctx: Context) = {
    val title = s"${user.username} • ${trans.exportGames.txt()}"
    views.html.base.layout(
      title = title,
      moreCss = cssTag("form3")
    ) {
      main(cls := "box page-small search")(
        h1(title),
        form(cls := "form3 box__pad")(
          table(
            tr(
              th(label(cls := "form-label")(trans.username())),
              td(st.input(tpe := "text", cls := "form-control", name := "username", value := s"${user.username}"))),

            trParam("since", "Since", "flatpickr"),
            trParam("until", "Until", "flatpick"),
            trParam("max", "Max", "input"),
            trParam("opponent", trans.opponent.txt(), "input"),
            trParam("rated", trans.rated.txt(), "checkbox"),
            tr(thHeader("ratingCategoryO", "Rating Category"),
              td(spanRatingPref("p-ultrabullet", "Ultrabullet"),
                spanRatingPref("p-bullet", "Bullet"),
                spanRatingPref("p-blitz", "Blitz"),
                spanRatingPref("p-rapid", "Rapid"),
                spanRatingPref("p-classical", "Classical"),
                spanRatingPref("p-correspondence", "Correspondence"),
                spanRatingPref("p-chess960", "Chess960"),
                spanRatingPref("p-crazyhouse", "Crazyhouse"),
                spanRatingPref("p-antichess", "Antichess"),
                spanRatingPref("p-atomic", "Atomic"),
                spanRatingPref("p-horde", "Horde"),
                spanRatingPref("p-koth", "King of the Hill"),
                spanRatingPref("p-racing-kings", "Racing Kings"),
                spanRatingPref("p-three-check", "Three Check")
              )
            ),
            tr(thHeader("colorO", "Color"),
              td(st.select(
                id := "colorV",
                cls := "form-control")(
                option(value:= trans.white.txt(),
                  selected)(trans.white()),
                  option(value:= trans.black.txt())(trans.black())
              ))),
            trParam("analysed", "Analysed", "checkbox"),
            trParam("moves", "Moves", "checkbox"),
            trParam("tags", "Tags", "checkbox"),
            trParam("clocks", "Clocks", "checkbox"),
            trParam("evals", "Evals", "checkbox"),
            trParam("opening", "Opening", "checkbox"),
          ),
          form3.split(
            span(id:= "unsetrcs", cls:= "button")("Unset all rating categories"),
            span(id:= "setrcs", cls:= "button")("Set all rating categories")),
          form3.split(
            div(id:= "clink", cls:= "button")("Create Link"),
            span(id:="link-wrap")("Link here")
          ),
          form3.split(
            st.input(
              tpe := "input",
              st.id := "clink-input"
            )
          )
        )
      )
    }
  }

  def trParam(id: String, name: String, tpe: String): Frag =
    tr(thHeader(id + "O", name), tdInput(id + "V", tpe))

  def tdInput(id: String, itype: String): Frag =
    td(st.input(tpe := itype,
      cls := "form-control",
      st.id := id))

  def thHeader(hid: String, name: String): Frag =
    th(span(cls := "form-check-input")(
      st.input(
        tpe := "checkbox",
        cls := "form-control cmn-toggle",
        st.id := hid,
        value := "1",
      ),
      label(`for` := hid)
    ),
      label(cls := "form-label", `for` := hid)(name)
    )

  def spanRatingPref(hid: String, name: String): Frag = 
    span(span(cls := "form-check-input")(
      st.input(
        tpe := "checkbox",
        cls := "form-control-small",
        st.id := hid,
        value := "1",
      ),
      label(`for` := hid)
    ),
      label(cls := "form-label", `for` := hid)(name)
    )

}
