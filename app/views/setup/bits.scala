package views.html.setup

import play.api.data.{ Form, Field }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

private object bits {

  val prefix = "sf_"

  def fenInput(field: Field, strict: Boolean, validFen: Option[lila.setup.ValidFen])(implicit ctx: Context) = {
    val url = field.value.fold(routes.Editor.index)(routes.Editor.load).url
    div(cls := "fen_position optional_config")(
      frag(
        div(cls := "fen_form", dataValidateUrl := s"""${routes.Setup.validateFen()}${strict.??("?strict=1")}""")(
          a(cls := "button thin hint--bottom", dataHint := trans.boardEditor.txt(), href := url)(iconTag("m")),
          form3.input(field)(st.placeholder := trans.pasteTheFenStringHere.txt())
        ),
        a(cls := "board_editor", href := url)(
          span(cls := "preview")(
            validFen.map { vf =>
              div(
                cls := "mini_board parse_fen is2d",
                dataColor := vf.color.name,
                dataFen := vf.fen.value,
                dataResizable := "1"
              )(miniBoardContent)
            }
          )
        )
      )
    )
  }

  def renderVariant(form: Form[_], variants: List[SelectChoice])(implicit ctx: Context) =
    div(cls := "variant label_select")(
      renderLabel(form("variant"), trans.variant.frag()),
      renderSelect(form("variant"), variants.filter {
        case (id, _, _) => ctx.noBlind || lila.game.Game.blindModeVariants.exists(_.id.toString == id)
      })
    )

  def renderSelect(
    field: Field,
    options: Seq[SelectChoice],
    compare: (String, String) => Boolean = (a, b) => a == b
  ) = select(id := s"$prefix${field.id}", name := field.name)(
    options.map {
      case (value, name, title) => option(
        st.value := value,
        st.title := title,
        selected := field.value.exists(v => compare(v, value)).option(true)
      )(name)
    }
  )

  def renderRadios(field: Field, options: Seq[SelectChoice]) =
    st.group(cls := "radio")(
      options.map {
        case (key, name, hint) => div(
          input(
            `type` := "radio",
            id := s"$prefix${field.id}_${key}",
            st.name := field.name,
            value := key,
            checked := field.value.has(key).option(true)
          ),
          label(
            cls := List("required" -> true, "hint--top" -> hint.isDefined),
            dataHint := hint,
            `for` := s"$prefix${field.id}_$key"
          )(name)
        )
      }
    )

  def renderInput(field: Field) =
    input(name := field.name, value := field.value, `type` := "hidden")

  def renderLabel(field: Field, content: Frag) =
    label(`for` := s"$prefix${field.id}")(content)

  def renderTimeMode(form: Form[_], config: lila.setup.BaseConfig)(implicit ctx: Context) =
    div(cls := "time_mode_config optional_config")(
      div(cls := "label_select")(
        renderLabel(form("timeMode"), trans.timeControl()),
        renderSelect(form("timeMode"), translatedTimeModeChoices)
      ),
      if (ctx.blind) frag(
        div(cls := "time_choice")(
          renderLabel(form("time"), trans.minutesPerSide()),
          renderSelect(form("time"), clockTimeChoices, (a, b) => a.replace(".0", "") == b)
        ),
        div(cls := "increment_choice")(
          renderLabel(form("increment"), trans.incrementInSeconds()),
          renderSelect(form("increment"), clockIncrementChoices)
        )
      )
      else frag(
        div(cls := "time_choice slider")(
          trans.minutesPerSide(),
          ": ",
          span(chess.Clock.Config(~form("time").value.map(x => (x.toDouble * 60).toInt), 0).limitString.toString),
          renderInput(form("time"))
        ),
        div(cls := "increment_choice slider")(
          trans.incrementInSeconds(),
          ": ",
          span(form("increment").value),
          renderInput(form("increment"))
        )
      ),
      div(cls := "correspondence")(
        if (ctx.blind) div(cls := "days_choice")(
          renderLabel(form("days"), trans.daysPerTurn()),
          renderSelect(form("days"), corresDaysChoices)
        )
        else div(cls := "days_choice slider")(
          trans.daysPerTurn(),
          ": ",
          span(form("days").value),
          renderInput(form("days"))
        )
      )
    )

  val dataRandomColorVariants =
    attr("data-random-color-variants") := lila.game.Game.variantsWhereWhiteIsBetter.map(_.id).mkString(",")

  val dataAnon = attr("data-anon")
  val dataMin = attr("data-min")
  val dataMax = attr("data-max")
  val dataValidateUrl = attr("data-validate-url")
  val dataResizable = attr("data-resizable")
  val dataType = attr("data-type")
}
