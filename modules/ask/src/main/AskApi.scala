package lila.ask

import lila.core.id.AskId
import lila.core.ask.*
import lila.core.ask.Ask.{ markerIdPrefix, markerIdRe }

// the encode process transforms form text prior to database storage and creates/updates collection
// objects. it returns replacement text with marker tags in place of any Ask markup found.
// decode methods allow editing by doing the inverse, replacing marker tags in a previously encoded
// text with their markup. ids in marker tags correspond to document ids in the ask collection.

final class AskApi(val repo: lila.ask.AskRepo)(using Executor) extends lila.core.ask.AskApi:

  import AskApi.*

  def encode(text: String, creator: UserId): Encoded =
    val askSpans = markupSpans(text)
    val asks = askSpans.map((start, end) => textToAsk(text.substring(start, end), creator))
    val it = asks.iterator
    val sb = java.lang.StringBuilder(text.length)

    intervalClosure(askSpans, text.length).map: seg =>
      if it.hasNext && askSpans.contains(seg) then sb.append(s"$markerIdPrefix{${it.next()._id}}\n")
      else sb.append(text, seg._1, seg._2)

    Encoded(sb.toString, asks)

  def commit(
      encoded: Encoded,
      url: Option[String] = none[String]
  ): Fu[Iterable[Ask]] =
    encoded.asks.map(ask => repo.upsert(ask.copy(url = url))).parallel

  def encodeAndCommit(text: String, creator: UserId, url: Option[String] = none[String]): Fu[String] =
    val askSpans = markupSpans(text)
    askSpans
      .map((start, end) => repo.upsert(textToAsk(text.substring(start, end), creator, url)))
      .parallel
      .map: asks =>
        val it = asks.iterator
        val sb = java.lang.StringBuilder(text.length)
        intervalClosure(askSpans, text.length).map: seg =>
          if it.hasNext && askSpans.contains(seg) then sb.append(s"$markerIdPrefix{${it.next()._id}}\n")
          else sb.append(text, seg._1, seg._2)
        sb.toString

  // decode methods replace marker ids with their actual markup to allow user edits
  def decode(text: String): Fu[String] =
    extractIds(text)
      .map(repo.getAsync)
      .parallel
      .map: asks =>
        val it = asks.iterator
        markerIdRe.replaceAllIn(
          text,
          _ => scala.util.matching.Regex.quoteReplacement(it.next().fold(askNotFoundFrag)(askToText))
        )

  def isOpen(aid: AskId): Fu[Boolean] = repo.isOpen(aid)

object AskApi:
  val askNotFoundFrag = "&lt;deleted&gt;<br>"
  val replaceMarkerRe = s"(?:<p>)?${markerIdRe.regex}(?:</p>)?".r

  def hasAskId(text: String): Boolean = text.contains(markerIdPrefix)

  // replaceMarkers embeds rendered ask htmls within the containing fragment html
  def replaceMarkers(outerFrag: String, askFrags: Iterable[String]): String =
    val sb = java.lang.StringBuilder(outerFrag.length + askFrags.foldLeft(0)((x, y) => x + y.length))
    val markerSpans = replaceMarkerRe.findAllMatchIn(outerFrag).map(m => (m.start, m.end)).toList
    val it = askFrags.iterator

    intervalClosure(markerSpans, outerFrag.length).map: span =>
      if it.hasNext && markerSpans.contains(span) then sb.append(it.next())
      else sb.append(outerFrag, span._1, span._2)
    sb.toString

  def extractIds(encoded: String): List[AskId] =
    markerSpans(encoded).map(off => lila.core.id.AskId(encoded.substring(off._1 + 5, off._2 - 1)))

  private def askToText(ask: Ask): String =
    val sb = scala.collection.mutable.StringBuilder(1024)
    sb ++= s"/ask ${ask.question}\n"
    sb ++= s"/id{${ask._id}}"
    if ask.isForm then sb ++= " form"
    if ask.isOpen then sb ++= " open"
    if ask.isTraceable then sb ++= " traceable"
    else
      if ask.isTally then sb ++= " tally"
      if ask.isAnon then sb ++= " anon"
    if ask.isVertical then sb ++= " vertical"
    if ask.isStretch then sb ++= " stretch"
    if ask.isRandom then sb ++= " random"
    if ask.isRanked then sb ++= " ranked"
    if ask.isMulti then sb ++= " multiple"
    if ask.isConcluded then sb ++= " concluded"
    sb ++= "\n"
    sb ++= ask.choices.map(c => s"$c\n").mkString
    sb ++= ~ask.footer.map(f => s"? $f\n")
    sb.toString

  private def textToAsk(markup: String, creator: UserId, url: Option[String] = none[String]): Ask =
    val tagString = extractTagString(markup)
    Ask.make(
      _id = extractIdFromTagString(tagString),
      question = extractQuestion(markup),
      choices = extractChoices(markup),
      tags = extractTagList(tagString.map(_.toLowerCase)),
      creator = creator,
      footer = extractFooter(markup),
      url = url
    )

  private type Span = (Int, Int) // [start, end)
  private type Spans = List[Span]

  // return intervals and their complement in [0, upper)
  private def intervalClosure(spans: Spans, upper: Int): Spans =
    val points =
      (0 :: spans.flatMap { case (begin, end) => List(begin, end) } ::: upper :: Nil).distinct.sorted
    points.zip(points.tail)

  private val markerLength = 14

  // build list of marker spans within an encoded text
  private def markerSpans(encoded: String): Spans =
    var i = encoded.indexOf(markerIdPrefix)
    if i == -1 then List.empty
    else
      val ids = scala.collection.mutable.ListBuffer[Span]()
      while i != -1 && i <= encoded.length - markerLength do
        ids.addOne(i, i + markerLength)
        i = encoded.indexOf(markerIdPrefix, i + markerLength)
      ids.toList

  private val askRe = raw"(?m)^/ask\h+\S.*\R^(?:/.*(?:\R|$$))?(?:(?!/).*\S.*(?:\R|$$))*(?:\?.*)?".r
  private val questionInAskRe = raw"^/ask\h+(\S.*)".r
  private val tagsInAskRe = raw"(?m)^/ask(?:.*)\R^/(.*)$$".r
  private val idInTagsRe = raw"\bid\{(\S{8})}".r
  private val tagListRe = raw"\h*(\S+)".r
  private val choiceInAskRe = raw"(?m)^(?![\?/])(.*\S.*)".r
  private val footerInAskRe = raw"(?m)^\?(.*)".r

  // return list of (start, end) indices of any ask markups in text.
  private def markupSpans(text: String): Spans =
    if !text.contains("/ask") then List.empty[Span]
    else askRe.findAllMatchIn(text).map(m => (m.start, m.end)).toList

  private def extractQuestion(markup: String): String =
    questionInAskRe.findFirstMatchIn(markup).fold("")(_.group(1)).trim

  private def extractTagString(markup: String): Option[String] =
    tagsInAskRe.findFirstMatchIn(markup).map(_.group(1)).filter(_.nonEmpty)

  private def extractIdFromTagString(tags: Option[String]): Option[String] =
    tags.flatMap(idInTagsRe.findFirstMatchIn(_).map(_.group(1)))

  private def extractTagList(tags: Option[String]): Ask.Tags =
    tags
      .fold(Set.empty[String])(
        tagListRe.findAllMatchIn(_).collect(_.group(1)).toSet
      )
      .filterNot(_.startsWith("id{"))

  private def extractChoices(markup: String): Ask.Choices =
    (choiceInAskRe.findAllMatchIn(markup).map(_.group(1).trim).distinct).toVector

  private def extractFooter(markup: String): Option[String] =
    footerInAskRe.findFirstMatchIn(markup).map(_.group(1).trim).filter(_.nonEmpty)
end AskApi
