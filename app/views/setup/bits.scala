package views.html.setup

import play.api.data.{ Field, Form }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

private object bits {

  val prefix = "sf_"

  def sfenInput(form: Form[_], strict: Boolean, validSfen: Option[lila.setup.ValidSfen])(implicit
      ctx: Context
  ) = {
    val positionChoices: List[SelectChoice] =
      List(
        ("default", trans.default.txt(), None),
        ("fromPosition", trans.fromPosition.txt(), None)
      )
    val variant = form("variant").value.flatMap(shogi.variant.Variant(_)) | shogi.variant.Standard
    val url = form("sfen").value
      .fold(routes.Editor.index)(sfen => routes.Editor.parseArg(s"${variant.key}/$sfen"))
      .url
    if (ctx.blind)
      div(
        cls             := "sfen_form",
        dataValidateUrl := s"""${routes.Setup.validateSfen}${strict.??("?strict=1")}"""
      )(
        renderLabel(form("sfen"), "SFEN"),
        renderSfenInput(form("sfen"))(st.placeholder := trans.default.txt())
      )
    else
      div(cls := "sfen_position optional_config")(
        renderRadios(form("position"), positionChoices),
        div(cls := "sfen_position_wrap")(
          div(cls := "handicap label_select")(
            renderLabel(form("handicap"), trans.handicap.txt()),
            renderSelect(form("handicap"), Nil),
            a(
              cls      := "button button-empty",
              dataIcon := "",
              title    := trans.handicap.txt(),
              target   := "_blank",
              href     := "https://en.wikipedia.org/wiki/Handicap_(shogi)"
            )
          ),
          div(
            cls             := "sfen_form",
            dataValidateUrl := s"""${routes.Setup.validateSfen}${strict.??("?strict=1")}"""
          )(
            renderLabel(form("sfen"), "SFEN"),
            renderSfenInput(form("sfen"))(st.placeholder := trans.pasteTheSfenStringHere.txt()),
            a(cls := "button button-empty", dataIcon := "m", title := trans.boardEditor.txt(), href := url)
          ),
          a(cls := "board_editor", href := url)(
            span(cls := "preview")(
              validSfen.map { vf =>
                div(
                  cls           := s"mini-board parse-sfen ${variantClass(vf.situation.variant)}",
                  dataColor     := vf.color.name,
                  dataSfen      := vf.sfen.value,
                  dataVariant   := vf.situation.variant.key,
                  dataResizable := "1"
                )(div(cls       := "sg-wrap"))
              }
            )
          )
        )
      )
  }

  def renderSfenInput(field: Field) =
    input(
      `type`  := "text",
      cls     := "form-control",
      id      := s"$prefix${field.id}",
      st.name := field.name
    )

  def renderVariant(form: Form[_], variants: List[SelectChoice])(implicit ctx: Context) =
    div(cls := "variant label_select")(
      renderLabel(form("variant"), trans.variant()),
      renderSelect(
        form("variant"),
        variants.filter { case (id, _, _) =>
          ctx.noBlind || lila.game.Game.blindModeVariants.exists(_.id.toString == id)
        }
      )
    )

  def renderSelect(
      field: Field,
      options: Seq[SelectChoice],
      compare: (String, String) => Boolean = (a, b) => a == b
  ) =
    select(id := s"$prefix${field.id}", name := field.name)(
      options.map { case (value, name, title) =>
        option(
          st.value := value,
          st.title := title,
          field.value.exists(v => compare(v, value)) option selected
        )(name)
      }
    )

  def renderRadios(field: Field, options: Seq[SelectChoice]) =
    st.group(cls := "radio")(
      options.map { case (key, name, hint) =>
        div(
          input(
            `type`  := "radio",
            id      := s"$prefix${field.id}_${key}",
            st.name := field.name,
            value   := key,
            field.value.has(key) option checked
          ),
          label(
            cls   := "required",
            title := hint,
            `for` := s"$prefix${field.id}_$key"
          )(name)
        )
      }
    )

  def renderInput(field: Field) =
    input(name := field.name, value := field.value, `type` := "hidden")

  def renderLabel(field: Field, content: Frag) =
    label(`for` := s"$prefix${field.id}")(content)

  def renderTimeMode(form: Form[_])(implicit ctx: Context) =
    div(cls := "time_mode_config optional_config")(
      div(cls := "label_select")(
        renderLabel(form("timeMode"), trans.timeControl()),
        renderSelect(
          form("timeMode"),
          if (ctx.isAuth) translatedTimeModeChoices else anonTranslatedTimeModeChoices
        )
      ),
      if (ctx.blind)
        frag(
          div(cls := "time_choice")(
            renderLabel(form("time"), trans.minutesPerSide()),
            renderSelect(form("time"), clockTimeChoices, (a, b) => a.replace(".0", "") == b)
          ),
          div(cls := "byoyomi_choice")(
            renderLabel(form("byoyomi"), trans.byoyomiInSeconds()),
            renderSelect(form("byoyomi"), clockByoyomiChoices)
          ),
          div(cls := "periods")(
            renderLabel(form("periods"), trans.periods()),
            renderSelect(form("periods"), periodsChoices)
          ),
          div(cls := "increment_choice")(
            renderLabel(form("increment"), trans.incrementInSeconds()),
            renderSelect(form("increment"), clockIncrementChoices)
          )
        )
      else
        frag(
          div(cls := "time_choice slider")(
            trans.minutesPerSide(),
            ": ",
            span(
              shogi.Clock.Config(~form("time").value.map(x => (x.toDouble * 60).toInt), 0, 0, 1).limitString
            ),
            renderInput(form("time"))
          ),
          div(cls := "byoyomi_choice slider")(
            trans.byoyomiInSeconds(),
            ": ",
            span(form("byoyomi").value),
            renderInput(form("byoyomi"))
          ),
          div(cls := "advanced_toggle"),
          div(cls := "advanced_setup")(
            div(cls := "periods buttons")(
              trans.periods(),
              div(id := "config_periods")(
                renderRadios(form("periods"), periodsChoices)
              )
            ),
            div(cls := "increment_choice slider")(
              trans.incrementInSeconds(),
              ": ",
              span(form("increment").value),
              renderInput(form("increment"))
            )
          )
        ),
      div(cls := "correspondence")(
        if (ctx.blind)
          div(cls := "days_choice")(
            renderLabel(form("days"), trans.daysPerTurn()),
            renderSelect(form("days"), translatedCorresDaysChoices)
          )
        else
          div(cls := "days_choice slider")(
            trans.daysPerTurn(),
            ": ",
            span(form("days").value),
            renderInput(form("days"))
          )
      )
    )

  val dataAnon        = attr("data-anon")
  val dataMin         = attr("data-min")
  val dataMax         = attr("data-max")
  val dataValidateUrl = attr("data-validate-url")
  val dataResizable   = attr("data-resizable")
  val dataType        = attr("data-type")
}
