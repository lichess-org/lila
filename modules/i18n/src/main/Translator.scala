package lila.i18n

import play.api.i18n.Lang
import scalatags.Text.all.*

import lila.common.String.html.escapeHtml
import lila.core.i18n.{ I18nKey, Translate, Translator, TranslatorFrag, TranslatorTxt }

object Translator extends Translator:

  def to(lang: Lang): Translate = Translate(this, lang)

  def toDefault: Translate = Translate(this, lila.core.i18n.defaultLang)

  object frag extends TranslatorFrag:
    def literal(key: I18nKey, args: Seq[Matchable], lang: Lang): RawFrag =
      translate(key, lang, I18nQuantity.Other /* grmbl */, args)

    def plural(key: I18nKey, count: Count, args: Seq[Matchable], lang: Lang): RawFrag =
      translate(key, lang, I18nQuantity(lang, count), args)

    private def translate(
        key: I18nKey,
        lang: Lang,
        quantity: I18nQuantity,
        args: Seq[Matchable]
    ): RawFrag =
      Registry
        .translation(lang, key)
        .flatMap: translation =>
          val htmlArgs = escapeArgs(args)
          try
            translation match
              case literal: Simple => Some(literal.format(htmlArgs))
              case literal: Escaped => Some(literal.format(htmlArgs))
              case plurals: Plurals => plurals.format(quantity, htmlArgs)
          catch
            case e: Exception =>
              logger.warn(s"Failed to format html $lang/$key -> $translation (${args.toList})", e)
              Some(RawFrag(key.value))
        .getOrElse(RawFrag(key.value))

    private def escapeArgs(args: Seq[Matchable]): Seq[RawFrag] = args.map:
      case s: String => escapeHtml(Html(s))
      case r: RawFrag => r
      case f: StringFrag => RawFrag(f.render)
      case a => RawFrag(a.toString)

  object txt extends TranslatorTxt:

    def literal(key: I18nKey, args: Seq[Any], lang: Lang): String =
      translate(key, lang, I18nQuantity.Other /* grmbl */, args)

    def plural(key: I18nKey, count: Count, args: Seq[Any], lang: Lang): String =
      translate(key, lang, I18nQuantity(lang, count), args)

    private def translate(
        key: I18nKey,
        lang: Lang,
        quantity: I18nQuantity,
        args: Seq[Any]
    ): String =
      Registry
        .translation(lang, key)
        .flatMap: translation =>
          try
            translation match
              case literal: Simple => Some(literal.formatTxt(args))
              case literal: Escaped => Some(literal.formatTxt(args))
              case plurals: Plurals => plurals.formatTxt(quantity, args)
          catch
            case e: Exception =>
              logger.warn(s"Failed to format txt $lang/$key -> $translation (${args.toList})", e)
              Some(key.value)
        .getOrElse(key.value)

  def duration(
      duration: java.time.Duration,
      withMinutes: Option[Boolean] = None,
      skipDays: Boolean = false
  )(using lang: Lang): String =
    val useMinutes = withMinutes.getOrElse(duration.toDays == 0 || skipDays)
    List(
      Option.unless(skipDays)(I18nKey.site.nbDays, true, duration.toDays),
      Some(I18nKey.site.nbHours, true, if skipDays then duration.toHours else duration.toHours % 24),
      Option.when(useMinutes)(I18nKey.site.nbMinutes, false, duration.toMinutes % 60)
    ).flatten
      .dropWhile { (_, dropZero, nb) => dropZero && nb == 0 }
      .map((key, _, nb) => txt.plural(key, nb, List(nb), lang))
      .mkString(", ")
