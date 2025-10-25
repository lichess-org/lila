package lila.ui

import play.api.i18n.Lang

import lila.core.i18n.{ I18nKey, LangList, Translator, fixJavaLanguage }
import lila.ui.ScalatagsTemplate.*

trait I18nHelper:

  protected val translator: Translator
  protected val ratingApi: lila.ui.RatingApi

  val langList: LangList

  extension (pk: PerfKey)
    def perfIcon: Icon = ratingApi.toIcon(pk)
    def perfName: I18nKey = ratingApi.toNameKey(pk)
    def perfDesc: I18nKey = ratingApi.toDescKey(pk)
    def perfTrans(using translate: Translate): String = perfName.txt()

  extension (v: chess.variant.Variant)
    def variantTrans: I18nKey = v match
      case chess.variant.Chess960 => I18nKey.variant.chess960
      case chess.variant.KingOfTheHill => I18nKey.variant.kingOfTheHill
      case chess.variant.ThreeCheck => I18nKey.variant.threeCheck
      case chess.variant.Antichess => I18nKey.variant.antichess
      case chess.variant.Atomic => I18nKey.variant.atomic
      case chess.variant.Horde => I18nKey.variant.horde
      case chess.variant.Crazyhouse => I18nKey.variant.crazyhouse
      case chess.variant.RacingKings => I18nKey.variant.racingKings
      case _ => I18nKey.variant.standard
    def variantTitleTrans: I18nKey = v match
      case chess.variant.Chess960 => I18nKey.variant.chess960Title
      case chess.variant.KingOfTheHill => I18nKey.variant.kingOfTheHillTitle
      case chess.variant.ThreeCheck => I18nKey.variant.threeCheckTitle
      case chess.variant.Antichess => I18nKey.variant.antichessTitle
      case chess.variant.Atomic => I18nKey.variant.atomicTitle
      case chess.variant.Horde => I18nKey.variant.hordeTitle
      case chess.variant.Crazyhouse => I18nKey.variant.crazyhouseTitle
      case chess.variant.RacingKings => I18nKey.variant.racingKingsTitle
      case _ => I18nKey.variant.standardTitle

  export lila.core.i18n.Translate
  export lila.core.i18n.I18nKey as trans
  export I18nKey.{ txt, pluralTxt, pluralSameTxt, apply, plural, pluralSame }

  given (using ctx: Context): Translate = Translate(translator, ctx.lang)
  given (using trans: Translate): Lang = trans.lang

  def transDefault: Translate = translator.toDefault

  def transKey(key: I18nKey, args: Seq[Matchable] = Nil)(using t: Translate): Frag =
    translator.frag.literal(key, args, t.lang)

  def langHref(call: Call)(using Context): String = langHref(call.url)
  def langHref(path: String)(using ctx: Context): String =
    if ctx.isAuth || ctx.lang.language == "en"
    then path
    else
      val code = fixJavaLanguage(ctx.lang)
      if path == "/" then s"/$code"
      else s"/$code$path"
