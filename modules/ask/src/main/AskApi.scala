package lila.ask

import scala.collection.mutable
import reactivemongo.api.bson._

import lila.db.dsl.{ *, given }
import lila.hub.actorApi.timeline.{ AskConcluded, Propagate }
import lila.user.User
import lila.security.Granter

/* an Ask is an object that represents a poll or quiz question together with its choices, tags
 * that specify its behavior & rendering, and an optional feedback field. Ask markup is the
 * text syntax used to define the ask.  the Freeze process transforms form text prior to
 * database storage and updates collection objects with ask markup.  freeze methods return Frozen
 * replacement text with magic{id} tags substituted for markup. Unfreeze methods allow editing by
 * substituting the markup back into a previously frozen text.
 */

final class AskApi(
    askDb: lila.db.AsyncColl,
    timeline: lila.hub.actors.Timeline
)(using scala.concurrent.ExecutionContext):

  import AskApi._

  given BSONDocumentHandler[Ask] = Macros.handler[Ask]

  def get(id: Ask.ID): Fu[Option[Ask]] = askDb: coll =>
    coll.byId[Ask](id)

  // eid (effective id) is a String (not a UserId) on purpose - they can be anonymous hashes.
  def setPicks(id: Ask.ID, eid: String, picks: Option[List[Int]]): Fu[Option[Ask]] =
    update(id, picks.fold($unset(s"picks.$eid"))(r => $set(s"picks.$eid" -> r)))

  def unset(id: Ask.ID, eid: String): Fu[Option[Ask]] =
    update(id, $unset(s"feedback.$eid", s"picks.$eid"))

  def setFeedback(id: Ask.ID, eid: String, feedback: Option[String]): Fu[Option[Ask]] =
    update(id, feedback.fold($unset(s"feedback.$eid"))(f => $set(s"feedback.$eid" -> f)))

  def conclude(ask: Ask): Fu[Option[Ask]] = conclude(ask._id)

  def conclude(id: Ask.ID): Fu[Option[Ask]] = askDb { coll =>
    coll.findAndUpdateSimplified[Ask](
      $id(id),
      $addToSet("tags" -> "concluded"),
      fetchNewObject = true
    ) collect { case Some(ask) =>
      timeline ! Propagate(AskConcluded(ask.creator, ask.question, ~ask.url))
        .toUsers(ask.participants.map(UserId(_)).toList)
        .exceptUser(ask.creator)
      ask.some
    }
  }

  def reset(ask: Ask): Fu[Option[Ask]] = reset(ask._id)

  def reset(id: Ask.ID): Fu[Option[Ask]] = askDb: coll =>
    coll.findAndUpdateSimplified[Ask](
      $id(id),
      $doc($unset("picks"), $pull("tags" -> "concluded")),
      fetchNewObject = true
    )

  def byUser(uid: UserId): Fu[List[Ask]] = askDb: coll =>
    coll.find($doc("creator" -> uid)).sort($sort desc "createdAt").cursor[Ask]().list(1000)

  def deleteAll(text: String): Funit = askDb: coll =>
    coll.delete.one($inIds(extractIds(text))).void

  // None values in the returned list are still important for sequencing
  def asksIn(text: String): Fu[List[Option[Ask]]] = askDb: coll =>
    coll.optionsByOrderedIds[Ask, Ask.ID](extractIds(text))(_._id)

  // freeze is synchronous but requires a subsequent async "commit" step that actually stores the asks
  def freeze(text: String, creator: User): Frozen =
    val askIntervals = getMarkupIntervals(text)
    val asks         = askIntervals.map((start, end) => textToAsk(text.substring(start, end), creator))

    val it = asks.iterator
    val sb = new java.lang.StringBuilder(text.length)

    intervalClosure(askIntervals, text.length).map: seg =>
      if it.hasNext && askIntervals.contains(seg) then sb.append(s"$frozenIdMagic{${it.next()._id}}")
      else sb.append(text, seg._1, seg._2)

    Frozen(sb.toString, asks)

  // commit flushes the asks to the db and optionally sets the timeline entry link (used on poll conclusion)
  def commit(frozen: Frozen, url: Option[String] = None): Fu[Iterable[Ask]] =
    frozen.asks map { ask => upsert(ask.copy(url = url)) } parallel

  // freezeAsync is freeze & commit together without the url.  call setUrl once you know it
  def freezeAsync(text: String, creator: User): Fu[Frozen] =
    val askIntervals = getMarkupIntervals(text)
    askIntervals
      .map((start, end) => upsert(textToAsk(text.substring(start, end), creator)))
      .parallel
      .map: asks =>
        val it = asks.iterator
        val sb = new java.lang.StringBuilder(text.length)

        intervalClosure(askIntervals, text.length).map: seg =>
          if it.hasNext && askIntervals.contains(seg) then sb.append(s"$frozenIdMagic{${it.next()._id}}")
          else sb.append(text, seg._1, seg._2)
        Frozen(sb.toString, asks)

  // call this after freezeAsync on form submission for edits
  def setUrl(frozen: String, url: Option[String]): Funit = askDb: coll =>
    if !hasAskId(frozen) then funit
    else coll.update.one($inIds(extractIds(frozen)), $set("url" -> url), multi = true).void

  // unfreeze replaces embedded ids in text with ask markup to allow user edits
  def unfreeze(text: String, asks: Iterable[Option[Ask]]): String =
    if asks.isEmpty then text
    else
      val it = asks.iterator
      frozenIdRe.replaceAllIn(text, _ => it.next().fold(askNotFoundFrag)(askToText))

  // unfreezeAsync when you can spare the time and don't have the asks handy
  def unfreezeAsync(text: String): Fu[String] =
    if !hasAskId(text) then fuccess(text)
    else asksIn(text).map(unfreeze(text, _))

  def isOpen(id: Ask.ID): Fu[Boolean] = askDb: coll =>
    coll.exists($and($id(id), $doc("tags" -> "open")))

  def hasAskId(text: String): Boolean                        = AskApi.hasAskId(text)
  def stripAsks(text: String, n: Int = -1): String           = AskApi.stripAsks(text, n)
  def bake(text: String, askFrags: Iterable[String]): String = AskApi.bake(text, askFrags)

  private def update(
      id: Ask.ID,
      update: BSONDocument
  ): Fu[Option[Ask]] = askDb: coll =>
    coll
      .findAndUpdateSimplified[Ask](
        selector = $and($id(id), $doc("tags" -> $ne("concluded"))),
        update = update,
        fetchNewObject = true
      ) flatMap:
        case None      => get(id) // in case it's concluded, look it up for the xhr response
        case Some(ask) => fuccess(ask.some)

  // only preserve votes if important fields haven't been altered
  private def upsert(ask: Ask): Fu[Ask] = askDb: coll =>
    coll
      .byId[Ask](ask._id) flatMap:
        case Some(dbAsk) =>
          val mergedAsk = ask.merge(dbAsk)
          if dbAsk eq mergedAsk then fuccess(dbAsk)
          else coll.update.one($id(ask._id), mergedAsk) inject mergedAsk
        // TODO - should probably call setUrl from ublog & post after updates in case a new ask was added
        case None =>
          coll.insert.one(ask) inject ask

  def delete(id: Ask.ID): Funit = askDb: coll =>
    coll.delete.one($id(id)).void

object AskApi:
  val askNotFoundFrag = "&lt;deleted&gt;<br>"

  case class Frozen(text: String, asks: Iterable[Ask])

  def hasAskId(text: String): Boolean = text.contains(frozenIdMagic)

  // remove frozen magic (for summaries)
  def stripAsks(text: String, n: Int = -1): String =
    frozenIdRe.replaceAllIn(text, "").take(if n == -1 then text.length else n)

  // combine text fragments with frozen text in proper order
  def bake(text: String, askFrags: Iterable[String]): String =
    val sb             = new mutable.StringBuilder(text.length + askFrags.foldLeft(0)((x, y) => x + y.length))
    val magicIntervals = frozenIdRe.findAllMatchIn(text).map(m => (m.start, m.end)).toList
    val it             = askFrags.iterator

    intervalClosure(magicIntervals, text.length).map: seg =>
      if (it.hasNext && magicIntervals.contains(seg)) sb.append(it.next())
      else sb.append(text, seg._1, seg._2)
    sb.toString

  // render ask as markup text
  private def askToText(ask: Ask): String =
    val sb = new mutable.StringBuilder(1024)
    sb ++= s"?? ${ask.question}\n"
    sb ++= s"?= id{${ask._id}}"
    if ask.isOpen then sb ++= " open"
    if ask.isTraceable then sb ++= " traceable"
    if ask.isTally then sb ++= " tally"
    if ask.isAnon then sb ++= " anonymized"
    if ask.isVertical then sb ++= " vertical"
    // if ask.isCenter then sb ++= " center"
    if ask.isStretch then sb ++= " stretch"
    if ask.isRandom then sb ++= " random"
    if ask.isRanked then sb ++= " ranked"
    if ask.isMulti then sb ++= " multiple"
    if ask.isFeedback then sb ++= " feedback"
    if ask.isConcluded then sb ++= " concluded"
    sb ++= "\n"
    sb ++= ask.choices.map(c => s"?# $c\n").mkString
    sb ++= s"${ask.footer.fold("")(r => s"?! $r\n")}"
    sb.toString

  // construct an Ask from the first markup in segment
  private def textToAsk(segment: String, creator: User): Ask =
    val params = extractParams(segment)
    Ask.make(
      _id = extractIdFromParams(params),
      question = extractQuestion(segment),
      choices = extractChoices(segment),
      tags = extractTagsFromParams(params map (_ toLowerCase)),
      creator = creator.id,
      footer = extractFooter(segment)
    )

  type Interval  = (Int, Int)     // [start, end) cleaner than regex match objects
  type Intervals = List[Interval] // for our purposes

  // return list of (start, end) indices of ask markups in text.
  private def getMarkupIntervals(t: String): Intervals =
    askRe findAllMatchIn t map (m => (m.start, m.end)) toList

  // return subs and their complement in [0, upper)
  private def intervalClosure(subs: Intervals, upper: Int): Intervals =
    val points = (0 :: subs.flatten(i => List(i._1, i._2)) ::: upper :: Nil).distinct.sorted
    points zip (points tail)

  // magic/id in a frozen text looks like:  ﷖﷔﷒﷐{8 char id}
  private def extractIds(t: String): List[Ask.ID] =
    var i   = t indexOf frozenIdMagic
    val ids = mutable.ListBuffer[String]()
    while (i != -1 && i <= t.length - 14)   // 14 is total magic length
      ids addOne t.substring(i + 5, i + 13) // (5, 13) delimit id within magic
      i = t.indexOf(frozenIdMagic, i + 14)
    ids toList

  // https://www.unicode.org/faq/private_use.html
  private val frozenIdMagic = "\ufdd6\ufdd4\ufdd2\ufdd0"
  private val frozenIdRe    = s"$frozenIdMagic\\{(\\S{8})}".r

  private def extractQuestion(t: String): String =
    questionInAskRe.findFirstMatchIn(t).get group 1 trim // NPE is desired if get fails

  private def extractParams(t: String): Option[String] =
    paramsInAskRe findFirstMatchIn t map (_ group 1)

  private def extractIdFromParams(o: Option[String]): Option[String] =
    o flatMap (idInParamsRe findFirstMatchIn _ map (_ group 1))

  private def extractTagsFromParams(o: Option[String]): Ask.Tags =
    o.fold(Set.empty[String])(
      tagsInParamsRe findAllMatchIn _ collect (_ group 1) toSet
    ) filterNot (_ startsWith "id{")

  private def extractChoices(t: String): Ask.Choices =
    (choicesInAskRe findAllMatchIn t map (_ group 1 trim) distinct) toVector

  private def extractFooter(t: String): Option[String] =
    footerInAskRe findFirstMatchIn t map (_ group 1 trim)

  private val askRe = (
    raw"(?m)^\?\?\h*\S.*\R"       // match "?? <question>" line and (
      + raw"((\?=.*\R?)?"         //      (match optional "?= <tags/params>" line and
      + raw"(\?#\h*\S.*\R?)+"     //       match 1 or more "?# <choice>" lines and
      + raw"(\?!.*\R?)?"          //       match optional "?! <footer>" line)
      + raw"|\?=.*feedback.*\R?)" //   or: match ?= with "feedback" param (a feedback-only ask)
  ).r                             // )
  private val questionInAskRe = raw"^\?\?(.*)".r
  private val paramsInAskRe   = raw"(?m)^\?=(.*)".r
  private val idInParamsRe    = raw"\bid\{(\S{8})}".r
  private val tagsInParamsRe  = raw"\h*(\S+)".r
  private val choicesInAskRe  = raw"(?m)^\?[#@](.*)".r
  private val footerInAskRe   = raw"(?m)^\?!(.*)".r
