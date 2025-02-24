package lila.i18n

import play.api.i18n.Lang

import scala.jdk.CollectionConverters.*

import lila.core.i18n.{ I18nKey, defaultLang }

class TranslationTest extends munit.FunSuite:

  Registry.syncLoadLanguages()

  given lila.core.i18n.Translator = lila.i18n.Translator

  test("be valid"):
    val en     = Registry.getAll(defaultLang).get
    var tested = 0
    val errors: List[String] = LangList.all.flatMap { (lang, name) =>
      Registry.getAll(lang).get.asScala.toMap.flatMap { (k, v) =>
        try
          val enTrans: String = en.get(k) match
            case literal: Simple  => literal.message
            case literal: Escaped => literal.message
            case plurals: Plurals => plurals.messages.getOrElse(I18nQuantity.Other, plurals.messages.head._2)
          val args = argsForKey(enTrans)
          v match
            case literal: Simple =>
              tested = tested + 1
              literal.formatTxt(args)
            case literal: Escaped =>
              tested = tested + 1
              literal.formatTxt(args)
            case plurals: Plurals =>
              plurals.messages.keys.foreach { qty =>
                tested = tested + 1
                assert(plurals.formatTxt(qty, args).nonEmpty)
              }
          None
        catch
          case _: MatchError => None // Extra translation
          case e: Exception  => Some(s"${lang.code} $name $k -> $v - ${e.getMessage}")
      }
    }.toList
    println(s"$tested translations tested")
    assertEquals(errors, Nil)
  test("escape html"):
    import scalatags.Text.all.*
    given Lang = defaultLang
    assertEquals(I18nKey.site.depthX("string"), RawFrag("Depth string"))
    assertEquals(I18nKey.site.depthX("<string>"), RawFrag("Depth &lt;string&gt;"))
    assertEquals(I18nKey.site.depthX(Html("<html>")), RawFrag("Depth &lt;html&gt;"))
  test("quotes"):
    given Lang = Lang("fr", "FR")
    assertEquals(
      I18nKey.faq.explainingEnPassant.txt("link1", "link2", "link3"),
      """Il s'agit d'un mouvement légal appelé "capture en passant" ou "prise en passant". L'article de Wikipedia donne un link1.

Il est décrit dans la section 3.7 (d) des link2 :

"Un pion occupant une case de la même rangée et sur une file adjacente à celle du pion adverse qui vient d'avancer de deux cases en un seul coup depuis sa case d'origine peut capturer le pion de cet adversaire comme si ce dernier n'avait été déplacé que d'une seule case. Cette capture n'est légale que lors du mouvement qui suit cette avance et est appelée "capture en passant"".

Voir les link3 sur ce coup pour vous entraîner."""
    )
  test("user backslashes"):
    given Lang = Lang("ar", "SA")
    assertEquals(
      I18nKey.faq.lichessCombinationLiveLightLibrePronounced.txt("link1"),
      """كلمة Lichess مزيج من live/light/libre (مباشر\خفيف\حر) و chess (شطرنج). تنطق link1."""
    )

  private def argsForKey(k: String): List[String] =
    if k.contains("%s") then List("arg1")
    else if k.contains("%6$s") then List("arg1", "arg2", "arg3", "arg4", "arg5", "arg6")
    else if k.contains("%5$s") then List("arg1", "arg2", "arg3", "arg4", "arg5")
    else if k.contains("%4$s") then List("arg1", "arg2", "arg3", "arg4")
    else if k.contains("%3$s") then List("arg1", "arg2", "arg3")
    else if k.contains("%2$s") then List("arg1", "arg2")
    else if k.contains("%1$s") then List("arg1")
    else Nil
