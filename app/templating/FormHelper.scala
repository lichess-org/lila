package lila.app
package templating

import play.api.data._

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._

trait FormHelper { self: I18nHelper =>

  def errMsg(form: Field)(implicit ctx: Context): Frag = errMsg(form.errors)

  def errMsg(form: Form[_])(implicit ctx: Context): Frag = errMsg(form.errors)

  def errMsg(error: FormError)(implicit ctx: Context): Frag =
    p(cls := "error")(transKey(error.message, error.args))

  def errMsg(errors: Seq[FormError])(implicit ctx: Context): Frag =
    errors map errMsg

  def globalError(form: Form[_])(implicit ctx: Context): Option[Frag] =
    form.globalError map errMsg

  val booleanChoices = Seq("true" -> "✓ Yes", "false" -> "✗ No")

  val postForm     = form(method := "post")
  val submitButton = button(tpe := "submit")

  object form3 {

    private val idPrefix = "form3"

    def id(field: Field): String = s"$idPrefix-${field.id}"

    private def groupLabel(field: Field) = label(cls := "form-label", `for` := id(field))
    private val helper                   = small(cls := "form-help")

    private def errors(errs: Seq[FormError])(implicit ctx: Context): Frag = errs map error
    private def errors(field: Field)(implicit ctx: Context): Frag         = errors(field.errors)
    private def error(err: FormError)(implicit ctx: Context): Frag =
      p(cls := "error")(transKey(err.message, err.args))

    private def validationModifiers(field: Field): Seq[Modifier] =
      field.constraints collect {
        /* Can't use constraint.required, because it applies to optional fields
         * such as `optional(nonEmptyText)`.
         * And we can't tell from the Field whether it's optional or not :(
         */
        // case ("constraint.required", _) => required
        case ("constraint.minLength", Seq(m: Int)) => minlength := m
        case ("constraint.maxLength", Seq(m: Int)) => maxlength := m
        case ("constraint.min", Seq(m: Int))       => min := m
        case ("constraint.max", Seq(m: Int))       => max := m
      }

    val split = div(cls := "form-split")

    def group(
        field: Field,
        labelContent: Frag,
        klass: String = "",
        half: Boolean = false,
        help: Option[Frag] = None
    )(content: Field => Frag)(implicit ctx: Context): Tag =
      div(
        cls := List(
          "form-group" -> true,
          "is-invalid" -> field.hasErrors,
          "form-half"  -> half,
          klass        -> klass.nonEmpty
        )
      )(
        groupLabel(field)(labelContent),
        content(field),
        errors(field),
        help map { helper(_) }
      )

    def input(field: Field, typ: String = "", klass: String = ""): BaseTagType =
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
          "form-half"             -> half
        )
      )(
        div(
          span(cls := "form-check-input")(
            cmnToggle(id(field), field.name, field.value.has("true"), disabled)
          ),
          groupLabel(field)(labelContent)
        ),
        help map { helper(_) }
      )

    def cmnToggle(
        fieldId: String,
        fieldName: String,
        checked: Boolean,
        disabled: Boolean = false,
        value: String = "true"
    ) =
      frag(
        st.input(
          st.id := fieldId,
          name := fieldName,
          st.value := value,
          tpe := "checkbox",
          cls := "form-control cmn-toggle",
          checked option st.checked,
          disabled option st.disabled
        ),
        label(`for` := fieldId)
      )

    def select(
        field: Field,
        options: Iterable[(Any, String)],
        default: Option[String] = None,
        disabled: Boolean = false
    ): Frag =
      frag(
        st.select(
          st.id := id(field),
          name := field.name,
          cls := "form-control"
        )(disabled option (st.disabled := true))(validationModifiers(field))(
          default map { option(value := "")(_) },
          options.toSeq map { case (value, name) =>
            option(
              st.value := value.toString,
              field.value.has(value.toString) option selected
            )(name)
          }
        ),
        disabled option hidden(field)
      )

    def textarea(
        field: Field,
        klass: String = ""
    )(modifiers: Modifier*): Frag =
      st.textarea(
        st.id := id(field),
        name := field.name,
        cls := List("form-control" -> true, klass -> klass.nonEmpty)
      )(validationModifiers(field))(modifiers)(~field.value)

    val actions = div(cls := "form-actions")
    val action  = div(cls := "form-actions single")

    def submit(
        content: Frag,
        icon: Option[String] = Some(""),
        nameValue: Option[(String, String)] = None,
        klass: String = "",
        confirm: Option[String] = None
    ): Tag =
      submitButton(
        dataIcon := icon,
        name := nameValue.map(_._1),
        value := nameValue.map(_._2),
        cls := List(
          "submit button" -> true,
          "text"          -> icon.isDefined,
          "confirm"       -> confirm.nonEmpty,
          klass           -> klass.nonEmpty
        ),
        title := confirm
      )(content)

    def hidden(field: Field, value: Option[String] = None): Tag =
      hidden(field.name, ~value.orElse(field.value))

    def hidden(name: String, value: String): Tag =
      st.input(
        st.name := name,
        st.value := value,
        tpe := "hidden"
      )

    def passwordModified(field: Field, content: Frag)(modifiers: Modifier*)(implicit ctx: Context): Frag =
      group(field, content)(input(_, typ = "password")(required)(modifiers))

    def passwordComplexityMeter(labelContent: Frag): Frag =
      div(cls := "password-complexity")(
        label(cls := "password-complexity-label")(labelContent),
        div(cls := "password-complexity-meter")(
          for (_ <- 1 to 4)
            yield span()
        )
      )

    def globalError(form: Form[_])(implicit ctx: Context): Option[Frag] =
      form.globalError map { err =>
        div(cls := "form-group is-invalid")(error(err))
      }

    def flatpickr(field: Field, withTime: Boolean = true, utc: Boolean = false): Tag =
      input(field, klass = s"flatpickr${if (utc) " flatpickr-utc" else ""}")(
        dataEnableTime := withTime,
        datatime24h := withTime
      )

    object file {
      def image(name: String): Frag = st.input(tpe := "file", st.name := name, accept := "image/*")
      def pgn(name: String): Frag   = st.input(tpe := "file", st.name := name, accept := ".pgn")
    }
  }
}
