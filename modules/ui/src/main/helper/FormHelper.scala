package lila.ui

import play.api.data.*
import play.api.i18n.Lang
import scalatags.generic.TypedTag
import scalatags.text.Builder

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.ui.Context
import lila.core.i18n.Translate
import lila.core.i18n.I18nKey as trans
import lila.ui.Icon

final class FormHelper(i18n: lila.ui.I18nHelper, flairApi: lila.core.user.FlairApi):

  import i18n.{ transKey, given }

  def errMsg(form: Field)(using Translate): Seq[Tag] = errMsg(form.errors)

  def errMsg(form: Form[?])(using Translate): Seq[Tag] = errMsg(form.errors)

  def errMsg(error: FormError)(using Translate): Tag =
    p(cls := "error")(transKey(trans(error.message), error.args))

  def errMsg(errors: Seq[FormError])(using Translate): Seq[Tag] =
    errors.map(errMsg)

  def globalError(form: Form[?])(using Translate): Option[Tag] =
    form.globalError.map(errMsg)

  def globalErrorNamed(form: Form[?], name: String)(using Translate): Option[Frag] =
    form.globalError.find(_.message == name).map(errMsg)

  val booleanChoices = Seq("true" -> "✓ Yes", "false" -> "✗ No")

  val postForm     = form(method := "post")
  val submitButton = button(tpe := "submit")

  def markdownAvailable(using Translate): Frag =
    trans.site.markdownAvailable:
      a(
        href := "https://guides.github.com/features/mastering-markdown/",
        targetBlank
      )("Markdown")

  def checkboxes[V](
      field: play.api.data.Field,
      options: Iterable[(V, String)],
      checked: Set[V],
      prefix: String = "op"
  ) = st.group(cls := "radio"):
    options.map { v =>
      val id = s"${field.id}_${v._1}"
      div(
        st.input(
          st.id := s"$prefix$id",
          checked(v._1).option(st.checked),
          tpe   := "checkbox",
          value := v._1.toString,
          name  := s"${field.name}[]"
        ),
        label(`for` := s"$prefix$id")(v._2)
      )
    }.toList

  val form3 = Form3(i18n, flairApi)
