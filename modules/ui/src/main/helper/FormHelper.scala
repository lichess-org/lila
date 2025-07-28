package lila.ui

import play.api.data.*
import scala.util.Try
import play.api.i18n.Lang
import scalalib.data.SimpleMemo
import lila.ui.ScalatagsTemplate.*

trait FormHelper:
  self: I18nHelper & AssetHelper =>

  protected def flairApi: lila.core.user.FlairApi

  lazy val form3 = Form3(this, flairApi)

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

  val postForm = form(method := "post")
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
          tpe := "checkbox",
          value := v._1.toString,
          name := s"${field.name}[]"
        ),
        label(`for` := s"$prefix$id")(v._2)
      )
    }.toList

  def translatedBooleanIntChoices(using Translate) =
    List(
      0 -> trans.site.no.txt(),
      1 -> trans.site.yes.txt()
    )
  def translatedBooleanChoices(using Translate) =
    List(
      false -> trans.site.no.txt(),
      true -> trans.site.yes.txt()
    )

  object timeZone:
    import java.time.{ ZoneId, ZoneOffset }
    import scala.jdk.CollectionConverters.*

    private val zones: SimpleMemo[List[(ZoneOffset, ZoneId)]] = SimpleMemo(67.minutes.some): () =>
      val now = nowInstant
      ZoneId.getAvailableZoneIds.asScala.toList
        .flatMap: id =>
          Try(ZoneId.of(id)).toOption
        .map: z =>
          (z.getRules.getOffset(now), z)
        .toList
        .sortBy: (offset, zone) =>
          (offset, zone.getId)

    def translatedChoices(using lang: Lang): List[(String, String)] =
      zones
        .get()
        .map: (offset, zone) =>
          zone.getId -> s"${zone.getDisplayName(java.time.format.TextStyle.NARROW, lang.locale)} $offset"
