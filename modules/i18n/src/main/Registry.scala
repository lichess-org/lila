package lila.i18n

import play.api.Mode
import play.api.i18n.Lang

import java.io.ObjectInputStream
import java.util.Map as JMap
import scala.jdk.CollectionConverters.*

import lila.common.Chronometer
import lila.core.i18n.{ I18nKey, defaultLang }

object Registry:

  private val empty: MessageMap          = java.util.HashMap[MessageKey, Translation]()
  private var all: Map[Lang, MessageMap] = Map[Lang, MessageMap]()
  private var default: MessageMap        = empty

  def translation(lang: Lang, key: I18nKey): Option[Translation] =
    all.get(lang).flatMap(t => Option(t.get(key))).orElse(Option(default.get(key)))

  def asyncLoadLanguages()(using Executor)(using scheduler: Scheduler, mode: Mode): Unit =
    scheduler.scheduleOnce(2.seconds):
      LangList.popular
        .grouped(10)
        .zipWithIndex
        .foreach: (langs, i) =>
          scheduler.scheduleOnce(i.seconds):
            val lap = Chronometer.sync:
              langs.foreach: lang =>
                register(lang, loadSerialized(lang))
            if i < 1 || mode.isProd
            then logger.info(s"Loaded ${langs.size} languages in ${lap.showDuration}")

  // for tests
  private[i18n] def getAll(lang: Lang): Option[MessageMap] = all.get(lang)
  private[i18n] def syncLoadLanguages(): Unit =
    LangList.popular.foreach: lang =>
      register(lang, loadSerialized(lang))

  private def register(lang: Lang, translations: MessageMap): Unit =
    all = all + (lang -> translations)
    if lang == defaultLang then default = translations
    lila.common.Bus.publish(lang, "i18n.load")

  private def loadSerialized(lang: Lang): MessageMap = try
    val file       = s"i18n.${lang.code}.ser"
    val istream    = ObjectInputStream(getClass.getClassLoader.getResourceAsStream(file))
    val messageMap = istream.readObject().asInstanceOf[JMap[String, Object]]
    istream.close()
    messageMap.asScala.toMap
      .map: (key, value) =>
        key -> value.match
          case s: String => singleOrEscaped(s)
          case m: JMap[?, ?] =>
            val plurals = m
              .asInstanceOf[JMap[String, String]]
              .asScala
              .flatMap: (q, i) =>
                I18nQuantity.fromString(q).map(_ -> i)
            Plurals(plurals.toMap)
          case _ => throw Exception(s"i18n oh noes $key: $value")
      .asJava
  catch
    case e: Exception =>
      logger.error(s"Failed to load i18n for ${lang.code}", e)
      empty

  private def singleOrEscaped(s: String) =
    val sb = java.lang.StringBuilder(s.length + 10)

    var dirty = false
    var i     = 0
    while i < s.length do
      s.charAt(i) match
        case '<' =>
          sb.append("&lt;")
          dirty = true
        case '>' =>
          sb.append("&gt;")
          dirty = true
        case '&' =>
          sb.append("&amp;")
          dirty = true
        case '"' =>
          sb.append("&quot;")
          dirty = true
        case '\'' =>
          sb.append("&#39;")
          dirty = true
        case '\n' =>
          sb.append("<br>")
          dirty = true
        case '\r' => dirty = true
        case c    => sb.append(c)
      i += 1
    if dirty then Escaped(s, sb.toString.replace("\\&quot;", "&quot;"))
    else Simple(s)
