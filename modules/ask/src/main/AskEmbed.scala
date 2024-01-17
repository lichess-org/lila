package lila.ask

import scala.collection.mutable

/* the freeze process transforms form text prior to database storage and creates/updates collection
 * objects with ask markup. freeze methods return replacement text with magic{id} tags in place
 * of any Ask markup found. unfreeze methods allow editing by doing the inverse, replacing magic
 * tags in a previously frozen text with their markup.
 */

final class AskEmbed(val repo: lila.ask.AskRepo)(using scala.concurrent.ExecutionContext):

  import AskEmbed.*
  import Ask.*

  def freeze(text: String, creator: UserId): Frozen =
    val askIntervals = getMarkupIntervals(text)
    val asks         = askIntervals.map((start, end) => textToAsk(text.substring(start, end), creator))

    val it = asks.iterator
    val sb = java.lang.StringBuilder(text.length)

    intervalClosure(askIntervals, text.length).map: seg =>
      if it.hasNext && askIntervals.contains(seg) then sb.append(s"$frozenIdMagic{${it.next()._id}}")
      else sb.append(text, seg._1, seg._2)

    Frozen(sb.toString, asks)

  // commit flushes the asks to repo and optionally sets the timeline entry link (for poll conclusion)
  def commit(frozen: Frozen, url: Option[String] = none[String]): Fu[Iterable[Ask]] =
    frozen.asks map { ask => repo.upsert(ask.copy(url = url)) } parallel

  def freezeAndCommit(text: String, creator: UserId, url: Option[String] = none[String]): Fu[String] =
    val askIntervals = getMarkupIntervals(text)
    askIntervals
      .map((start, end) => repo.upsert(textToAsk(text.substring(start, end), creator, url)))
      .parallel
      .map: asks =>
        val it = asks.iterator
        val sb = java.lang.StringBuilder(text.length)

        intervalClosure(askIntervals, text.length).map: seg =>
          if it.hasNext && askIntervals.contains(seg) then sb.append(s"$frozenIdMagic{${it.next()._id}}")
          else sb.append(text, seg._1, seg._2)
        sb.toString

  // unfreeze methods replace magic ids with their ask markup to allow user edits
  def unfreezeAndLoad(text: String): Fu[String] =
    extractIds(text)
      .map(repo.getAsync)
      .parallel
      .map: asks =>
        val it = asks.iterator
        frozenIdRe.replaceAllIn(text, _ => it.next().fold(askNotFoundFrag)(askToText))

  // dont call this without preloading first
  def unfreeze(text: String): String =
    val it = extractIds(text).map(repo.get).iterator
    frozenIdRe.replaceAllIn(text, _ => it.next().fold(askNotFoundFrag)(askToText))

  def isOpen(aid: Ask.ID): Fu[Boolean] = repo.isOpen(aid)

  def stripAsks(text: String, n: Int = -1): String           = AskEmbed.stripAsks(text, n)
  def bake(text: String, askFrags: Iterable[String]): String = AskEmbed.bake(text, askFrags)

object AskEmbed:
  val askNotFoundFrag = "&lt;deleted&gt;<br>"

  case class Frozen(text: String, asks: Iterable[Ask])

  def hasAskId(text: String): Boolean = text.contains(frozenIdMagic)

  // remove frozen magic (for summaries)
  def stripAsks(text: String, n: Int = -1): String =
    frozenIdRe.replaceAllIn(text, "").take(if n == -1 then text.length else n)

  // combine text fragments with frozen text in proper order
  def bake(html: String, askFrags: Iterable[String]): String =
    val tag = if html.slice(0, 1) == "<" then html.slice(1, html.indexOf(">")) else ""
    val sb  = new java.lang.StringBuilder(html.length + askFrags.foldLeft(0)((x, y) => x + y.length))
    val magicIntervals = frozenIdRe.findAllMatchIn(html).map(m => (m.start, m.end)).toList
    val it             = askFrags.iterator

    intervalClosure(magicIntervals, html.length).map: seg =>
      val text = html.substring(seg._1, seg._2)
      if it.hasNext && magicIntervals.contains(seg) then
        if tag.nonEmpty then sb.append(s"</$tag>")
        sb.append(it.next)
      else if !(text.isBlank() || text.startsWith(s"</$tag>")) then
        sb.append(if seg._1 > 0 && tag.nonEmpty then s"<$tag>$text" else text)
    sb.toString

  def tag(html: String) = html.slice(1, html.indexOf(">"))

  def extractIds(t: String): List[Ask.ID] =
    frozenOffsets(t) map (off => t.substring(off._1 + 5, off._2 - 1))

  // render ask as markup text
  private def askToText(ask: Ask): String =
    val sb = new mutable.StringBuilder(1024)
    sb ++= s"/poll ${ask.question}\n"
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

  private def textToAsk(segment: String, creator: UserId, url: Option[String] = none[String]): Ask =
    val tagString = extractTagString(segment)
    Ask.make(
      _id = extractIdFromTagString(tagString),
      question = extractQuestion(segment),
      choices = extractChoices(segment),
      tags = extractTagList(tagString map (_ toLowerCase)),
      creator = creator,
      footer = extractFooter(segment),
      url = url
    )

  type Interval  = (Int, Int) // [start, end) cleaner than regex match objects for our purpose
  type Intervals = List[Interval]

  // return list of (start, end) indices of ask markups in text.
  private def getMarkupIntervals(t: String): Intervals =
    if !t.contains("/poll") then Nil
    else askRe findAllMatchIn t map (m => (m.start, m.end)) toList

  // return intervals and their complement in [0, upper)
  private def intervalClosure(intervals: Intervals, upper: Int): Intervals =
    val points = (0 :: intervals.flatten(i => List(i._1, i._2)) ::: upper :: Nil).distinct.sorted
    points zip (points tail)

  // https://www.unicode.org/faq/private_use.html
  private val frozenIdMagic = "\ufdd6\ufdd4\ufdd2\ufdd0"
  private val frozenIdRe    = s"$frozenIdMagic\\{(\\S{8})}".r

  // magic/id in a frozen text looks like:  ﷖﷔﷒﷐{8 char id}
  private def frozenOffsets(t: String): List[Interval] =
    var i = t indexOf frozenIdMagic
    if i == -1 then Nil
    else
      val ids = mutable.ListBuffer[Interval]()
      while i != -1 && i <= t.length - 14 do // 14 is total magic length
        ids addOne (i, i + 14)               // (5, 13) delimit id within magic
        i = t.indexOf(frozenIdMagic, i + 14)
      ids toList

  private def extractQuestion(t: String): String =
    questionInAskRe.findFirstMatchIn(t).fold("")(_ group 1) trim

  private def extractTagString(t: String): Option[String] =
    tagsInAskRe findFirstMatchIn t map (_ group 1) filter (_.nonEmpty)

  private def extractIdFromTagString(o: Option[String]): Option[String] =
    o flatMap (idInTagsRe findFirstMatchIn _ map (_ group 1))

  private def extractTagList(o: Option[String]): Ask.Tags =
    o.fold(Set.empty[String])(
      tagListRe findAllMatchIn _ collect (_ group 1) toSet
    ) filterNot (_ startsWith "id{")

  private def extractChoices(t: String): Ask.Choices =
    (choiceInAskRe findAllMatchIn t map (_ group 1 trim) distinct) toVector

  private def extractFooter(t: String): Option[String] =
    footerInAskRe findFirstMatchIn t map (_ group 1 trim) filter (_.nonEmpty)

  private val askRe           = raw"(?m)^/poll\h+\S.*\R^(?:/.*(?:\R|$$))?(?:(?!/).*\S.*(?:\R|$$))*(?:\?.*)?".r
  private val questionInAskRe = raw"^/poll\h+(\S.*)".r
  private val tagsInAskRe     = raw"(?m)^/poll(?:.*)\R^/(.*)$$".r
  private val idInTagsRe      = raw"\bid\{(\S{8})}".r
  private val tagListRe       = raw"\h*(\S+)".r
  private val choiceInAskRe   = raw"(?m)^(?![\?/])(.*\S.*)".r
  private val footerInAskRe   = raw"(?m)^\?(.*)".r
