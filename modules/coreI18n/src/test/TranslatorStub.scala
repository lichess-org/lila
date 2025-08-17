package lila.core.i18n

import play.api.i18n.Lang

val TranslatorStub = new Translator:
  def to(lang: Lang): Translate = Translate(this, lang)
  def toDefault: Translate = Translate(this, defaultLang)
  val txt = new TranslatorTxt:
    def literal(key: I18nKey, args: Seq[Any], lang: Lang): String = key.value
    def plural(key: I18nKey, count: Count, args: Seq[Any], lang: Lang): String = key.value
  val frag = new TranslatorFrag:
    import scalatags.Text.RawFrag
    def literal(key: I18nKey, args: Seq[Matchable], lang: Lang): RawFrag = RawFrag(key.value)
    def plural(key: I18nKey, count: Count, args: Seq[Matchable], lang: Lang): RawFrag = RawFrag(key.value)
