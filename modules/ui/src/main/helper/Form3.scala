package lila.ui

import play.api.data.*
import scalatags.Text.TypedTag

import lila.core.i18n.{ I18nKey as trans, Translate }
import lila.core.user.FlairApi
import lila.ui.ScalatagsTemplate.{ *, given }

final class Form3(formHelper: FormHelper & I18nHelper & AssetHelper, flairApi: FlairApi):
  import formHelper.{ transKey, given }

  private val idPrefix = "form3"

  def id(field: Field): String = s"$idPrefix-${field.id}"

  private def groupLabel(field: Field) = label(cls := "form-label", `for` := id(field))
  private val helper = small(cls := "form-help")

  def errors(field: Field)(using Translate): Frag = errors(field.errors)
  private def errors(errs: Seq[FormError])(using Translate): Frag = errs.distinct.map(error)
  private def error(err: FormError)(using Translate): Frag =
    p(cls := "error")(transKey(trans(err.message), err.args))

  private def validationModifiers(field: Field): Seq[Modifier] =
    field.constraints.collect:
      /* Can't use constraint.required, because it applies to optional fields
       * such as `optional(nonEmptyText)`.
       * And we can't tell from the Field whether it's optional or not :(
       */
      // case ("constraint.required", _) => required
      case ("constraint.minLength", Seq(m: Int)) => minlength := m
      case ("constraint.maxLength", Seq(m: Int)) => maxlength := m
      case ("constraint.min", Seq(m: Int)) => min := m
      case ("constraint.max", Seq(m: Int)) => max := m

  val split = div(cls := "form-split")

  def group(
      field: Field,
      labelContent: Frag,
      klass: String = "",
      half: Boolean = false,
      help: Option[Frag] = None
  )(content: Field => Frag)(using Translate): Tag =
    div(
      cls := List(
        "form-group" -> true,
        "is-invalid" -> field.hasErrors,
        "form-half" -> half,
        klass -> klass.nonEmpty
      )
    )(
      groupLabel(field)(labelContent),
      content(field),
      errors(field),
      help.map { helper(_) }
    )

  def input(field: Field, typ: String = "", klass: String = "") /*: BaseTagType*/ =
    st.input(
      st.id := id(field),
      name := field.name,
      value := field.value,
      tpe := typ.nonEmpty.option(typ),
      cls := List("form-control" -> true, klass -> klass.nonEmpty)
    )(validationModifiers(field))

  def checkbox(
      field: Field,
      labelContent: Frag,
      half: Boolean = false,
      help: Option[Frag] = None,
      disabled: Boolean = false
  ): Frag =
    div(
      cls := List(
        "form-check form-group" -> true,
        "form-half" -> half
      )
    )(
      div(
        span(cls := "form-check-input")(
          cmnToggle(id(field), field.name, isChecked(field), disabled)
        ),
        groupLabel(field)(labelContent)
      ),
      help.map { helper(_) }
    )

  def cmnToggle[Value: Show](
      fieldId: String,
      fieldName: String,
      checked: Boolean,
      disabled: Boolean = false,
      value: Value = "true",
      styleClass: String = "cmn-toggle",
      title: Option[String] = None,
      action: Option[String] = None
  ) =
    frag(
      (disabled && checked).option: // disabled checkboxes don't submit; need an extra hidden field
        hidden(fieldName, value)
      ,
      st.input(
        st.id := fieldId,
        name := fieldName,
        st.value := value.show,
        tpe := "checkbox",
        cls := s"form-control $styleClass",
        checked.option(st.checked),
        disabled.option(st.disabled),
        action.map(st.data("action") := _)
      ),
      label(`for` := fieldId, title.map(st.title := _))
    )

  def nativeCheckbox[Value: Show](
      fieldId: String,
      fieldName: String,
      checked: Boolean,
      value: Value = "true",
      disabled: Boolean = false
  ) =
    span(cls := "form-check__input")(
      st.input(
        st.id := fieldId,
        name := fieldName,
        st.value := value.show,
        tpe := "checkbox",
        checked.option(st.checked),
        disabled.option(st.disabled)
      ),
      label(cls := "form-check__label", `for` := fieldId)
    )

  def checkboxGroup(
      field: Field,
      labelContent: Frag,
      half: Boolean = false,
      help: Option[Frag] = None,
      value: String = "true",
      disabled: Boolean = false,
      klass: String = ""
  ): Frag =
    div(
      cls := List(
        "form-check form-group" -> true,
        "form-half" -> half,
        klass -> klass.nonEmpty
      )
    )(
      div(cls := "form-check__container")(
        nativeCheckbox(
          id(field),
          field.name,
          checked = isChecked(field),
          value = value,
          disabled = disabled
        ),
        groupLabel(field)(labelContent)
      ),
      help.map { helper(_) }
    )

  def isChecked(field: Field): Boolean =
    field.value.exists(v => v == "true" || v == "1")

  def select(
      field: Field,
      options: Iterable[(Any, String)],
      default: Option[String] = None,
      disabled: Boolean = false,
      required: Boolean = false
  ): Frag =
    frag(
      st.select(
        st.id := id(field),
        name := field.name,
        cls := "form-control",
        disabled.option(st.disabled),
        required.option(st.required)
      )(validationModifiers(field))(
        default.map { option(value := "")(_) },
        options.toSeq.map { (value, name) =>
          option(
            st.value := value.toString,
            field.value.has(value.toString).option(selected)
          )(name)
        }
      ),
      disabled.option(hidden(field))
    )

  def textarea(
      field: Field,
      klass: String = ""
  )(modifiers: Modifier*): Tag =
    st.textarea(
      st.id := id(field),
      name := field.name,
      cls := List("form-control" -> true, klass -> klass.nonEmpty)
    )(validationModifiers(field))(modifiers)((field.value.orZero: String))

  val actions = div(cls := "form-actions")
  val action = div(cls := "form-actions single")

  def submit(
      content: Frag,
      icon: Option[Icon] = Some(Icon.Checkmark),
      nameValue: Option[(String, String)] = None,
      confirm: Option[String] = None
  ): Tag =
    button(
      tpe := "submit",
      dataIcon := icon,
      name := nameValue._1F,
      value := nameValue._2F,
      cls := List(
        "submit button" -> true,
        "text" -> icon.isDefined,
        "yes-no-confirm" -> confirm.nonEmpty
      ),
      title := confirm
    )(content)

  def hidden(field: Field): Tag =
    hidden(field.name, ~field.value)

  def hidden[Value: Show](field: Field, value: Option[Value] = None): Tag =
    hidden(field.name, ~value.map(_.show).orElse(field.value))

  def hidden[Value: Show](name: String, value: Option[Value]): Tag =
    st.input(
      st.name := name,
      st.value := value.map(_.show),
      tpe := "hidden"
    )

  def hidden[Value: Show](name: String, value: Value): Tag =
    st.input(
      st.name := name,
      st.value := value.show,
      tpe := "hidden"
    )

  // allows disabling of a field that defaults to true
  def hiddenFalse(field: Field): Tag = hidden(field, "false".some)

  def passwordModified(field: Field, content: Frag, reveal: Boolean = true, half: Boolean = false)(
      modifiers: Modifier*
  )(using Translate): Frag =
    group(field, content, half = half): f =>
      div(cls := "password-wrapper")(
        input(f, typ = "password")(required)(modifiers),
        reveal.option(button(cls := "password-reveal", tpe := "button", dataIcon := Icon.Eye))
      )

  def passwordComplexityMeter(labelContent: Frag): Frag =
    div(cls := "password-complexity")(
      label(cls := "password-complexity-label")(labelContent),
      div(cls := "password-complexity-meter"):
        for _ <- 1 to 4 yield span
    )

  def globalError(form: Form[?])(using Translate): Option[Frag] =
    form.globalError.map: err =>
      div(cls := "form-group is-invalid")(error(err))

  def fieldset(legend: Frag, toggle: Option[Boolean] = none): Tag =
    st.fieldset(
      cls := List(
        "toggle-box" -> true,
        "toggle-box--toggle" -> toggle.isDefined,
        "toggle-box--toggle-off" -> toggle.has(false)
      )
    )(st.legend(toggle.map(_ => tabindex := 0))(legend))

  private val dataEnableTime = attr("data-enable-time")
  private val dataMinDate = attr("data-min-date")
  private val dataLocal = attr("data-local")

  def flatpickr(
      field: Field,
      withTime: Boolean = true,
      local: Boolean = false,
      minDate: Option[String] = Some("today")
  ): Tag =
    input(field, klass = s"flatpickr")(
      withTime.option(dataEnableTime := true),
      local.option(dataLocal := true),
      dataMinDate := minDate.map:
        case "today" if local => "yesterday"
        case d => d
    )

  private lazy val exceptEmojis = data("except-emojis") := flairApi.adminFlairs.mkString(" ")
  def flairPickerGroup(field: Field, current: Option[Flair])(using Context): Tag =
    group(field, trans.site.flair(), half = true): f =>
      flairPicker(f, current)

  def flairPicker(field: Field, current: Option[Flair], anyFlair: Boolean = false)(using
      ctx: Context
  ): Frag =
    frag(
      div(cls := "form-control emoji-details")(
        div(cls := "emoji-popup-button")(
          st.select(st.id := id(field), name := field.name, cls := "form-control")(
            current.map(f => option(value := f, selected := ""))
          ),
          img(src := current.fold(Url(""))(formHelper.flairSrc(_)))
        ),
        div(
          cls := "flair-picker none",
          (!Granter.opt(_.LichessTeam) && !anyFlair).option(exceptEmojis)
        )(
          button(cls := "button button-metal emoji-remove")("clear")
        )
      )
    )

  object file:
    def image(name: String): Frag =
      st.input(tpe := "file", st.name := name, accept := "image/png, image/jpeg, image/webp")
    def pgn(name: String): Frag = st.input(tpe := "file", st.name := name, accept := ".pgn")
    def selectImage = button(cls := "button select-image", tpe := "button")("Select image")
