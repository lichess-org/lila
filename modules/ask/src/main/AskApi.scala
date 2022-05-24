package lila.ask

import scala.concurrent.Future
import reactivemongo.api.bson._
import lila.db.dsl._
import lila.hub.actors.Timeline
import lila.user.User
import lila.ask.Ask.imports._
import lila.common.Markdown
import lila.hub.actorApi.timeline.{ AskConcluded, Propagate }

final class AskApi(
    coll: Coll,
    timeline: Timeline
)(implicit ec: scala.concurrent.ExecutionContext) {

  import AskApi._

  implicit val AskBSONHandler = Macros.handler[Ask]

  /** Gets an ask object from the db
    *
    * @return
    *   future containing ask, or None if not found
    * @param id
    *   id of the ask object
    */
  def get(id: Ask.ID): Fu[Option[Ask]] =
    coll.byId[Ask](id)

  /** Submits/removes a user pick for an ask
    *
    * @return
    *   future containing updated ask, or None if not found
    * @param id
    *   id of the ask object
    * @param uid
    *   id of the user
    * @param pick
    *   integer index into Ask.choices list, or None to remove user pick
    */

  def pick(id: Ask.ID, uid: User.ID, pick: Option[Int]): Fu[Option[Ask]] =
    coll.ext.findAndUpdate[Ask](
      selector = $and($id(id), $doc("isConcluded" -> false)),
      update = {
        pick.fold($unset(s"picks.$uid"))(p => $set(s"picks.$uid" -> p))
      },
      fetchNewObject = true
    ) flatMap {
      case None =>
        get(id) // was concluded prior to the pick. look up the ask.
      case ask =>
        fuccess(ask)
    }

  /** Concludes an ask and notifies participants that results available for viewing via timeline
    *
    * @return
    *   the ask or None if not found
    * @param id
    *   id of the ask object
    * @param href
    *   provide a uri for notifications (where results can be viewed)
    */
  def conclude(ask: Ask): Fu[Option[Ask]] = conclude(ask._id)

  def conclude(id: Ask.ID): Fu[Option[Ask]] =
    coll.ext.findAndUpdate[Ask]($id(id), $set("isConcluded" -> true), fetchNewObject = true).map {
      case Some(ask) =>
        timeline ! Propagate(AskConcluded(ask.creator, ask.question, ~ask.url))
          .toUsers(ask.participants.toList)
          .exceptUser(ask.creator)
        ask.some
      case None => None
    }

  /** Resets the picks on an ask
    * @param id
    *   the ask to reset
    * @return
    *   ask with no picks
    */

  def reset(ask: Ask): Fu[Option[Ask]] = reset(ask._id)

  def reset(id: Ask.ID): Fu[Option[Ask]] =
    coll.ext.findAndUpdate[Ask]($id(id), $unset("picks"), fetchNewObject = true)

  /** Deletes all asks associated with a cookie
    *
    * @param cookie
    */

  def deleteAsks(cookie: Option[Ask.Cookie]): Funit =
    coll.delete.one($inIds(extractIds(cookie))).void

  /** Updates markup in input text, updates cookie, and reconciles any ask markup with db. Call this before
    * creating your database object whenever a form is submitted. make sure this completes before calling
    * render. Also, at some point the client should provide a URL for each ask, this can be done in render or
    * at some other time via setUrl.
    *
    * @return
    *   future Updated(annotatedText, Some(cookie)) or (formText, None) if no ask markup
    * @param formText
    *   annotate formText markup (if any) with ids and return annotatedText
    * @param creator
    *   the user id who posted this formText
    * @param cookie
    *   existing ask cookie or None
    * @param isMarkdown
    *   if markdown has gotten its filthy hands on this text, pass true here.
    */
  def prepare(
      formText: String,
      creator: User.ID,
      cookie: Option[Ask.Cookie] = None,
      isMarkdown: Boolean = false
  ): Fu[Updated] = {

    val stripFunc: String => String =
      if (isMarkdown) stripMarkdownEscapes else { x => x }

    val annotated     = new StringBuilder
    val markupOffsets = getMarkupOffsets(formText)
    val (idList, opList) = getIntervalClosure(markupOffsets, formText.length).map { interval =>
      val txt = formText.slice(interval._1, interval._2)
      if (!markupOffsets.contains(interval)) {
        annotated ++= txt
        ("", funit)
      } else {
        val params = extractParams(txt)
        val ask = Ask.make(
          _id = extractId(txt),
          question = stripFunc(extractQuestion(txt)),
          choices = extractChoices(txt) map stripFunc,
          isPublic = params.fold(false)(_ contains "public"),
          isTally = params.fold(false)(_ contains "tally"),
          isConcluded = params.fold(false)(_ contains "concluded"),
          creator = creator,
          answer = extractAnswer(txt) map stripFunc,
          reveal = extractReveal(txt) map stripFunc
        )
        annotated ++= askToText(ask)
        (ask._id, update(ask))
      }
    }.unzip
    extractIds(cookie).filterNot(idList contains) map delete

    Future.sequence(opList) inject
      Updated(annotated.toString, makeV1Cookie(idList filter (_.nonEmpty)))
  }

  /** Generates a sequence of ask objects and unmarked text segments for view construction. RenderElement is
    * an alias for Either[Ask, String], and isAsk/isText alias Left/Right for clearer match expressions
    *
    * renderSeq map { case isAsk(ask) => ... case isText(txt) => }
    *
    * @param text
    *   annotated text provided by askApi.update
    * @param url
    *   if there is a redirect url and it was not provided in prepare, it is recommended you pass it here
    *   rather than separately in setUrl (to minimize database load)
    * @return
    *   future sequence of RenderElement (either ask or text)
    */
  def render(
      text: String,
      url: Option[String],
      formatter: Option[String => String] = None
  ): Fu[Seq[Ask.RenderElement]] = {

    val markupOffsets = getMarkupOffsets(text)
    val opList = getIntervalClosure(markupOffsets, text.length) map { interval =>
      val txt = text.slice(interval._1, interval._2)

      if (!markupOffsets.contains(interval))
        fuccess(isText(formatter.fold(txt)(_(txt))))
      else {
        get(extractId(txt).get) flatMap { // if gets fail, somebody didn't call prepare so NPE
          case None =>
            fuccess(isText("[deleted]"))
          case Some(ask) =>
            url.map(setUrl(ask, _))
            fuccess(isAsk(ask))
        }
      }
    }
    Future.sequence(opList)
  }

  /** Sets the url for all asks in the provided cookie
    *
    * @param cookie
    *   cookie identifying the ask(s)
    * @param url
    *   timeline entries generated for participants will link to this url on ask conclusion when appropriate
    */
  def setUrl(cookie: Option[Ask.Cookie], url: String): Funit =
    coll.update.one($inIds(extractIds(cookie)), $set("url" -> url), multi = true).void

  /** returns a summary of user selections for all asks in a given cookie useful for quizzes and
    * questionnaires (?)
    * @param cookie
    *   to summarize
    * @param uid
    *   to summarize
    * @return
    *   a future sequence of question/answer strings
    */
  def getUserSummary(cookie: Option[Ask.Cookie], uid: User.ID): Fu[Seq[String]] =
    coll
      .find($inIds(extractIds(cookie)))
      .cursor[Ask]()
      .list()
      .map(askList =>
        askList map { ask =>
          ask.getPick(uid).fold("") { i =>
            val choice = ask.choices(i)
            s"${ask.question}:  $choice" +
              ask.answer.fold("")(a => (a != choice) ?? s" ($a)")
          }
        }
      )

  private def update(ask: Ask): Funit =
    coll.byId[Ask](ask._id) flatMap {
      case Some(oldAsk) =>
        if (oldAsk.invalidatedBy(ask))
          coll.update.one($id(ask._id), ask).void
        else if (oldAsk.isTally != ask.isTally || oldAsk.isConcluded != ask.isConcluded)
          coll.update
            .one(
              $id(ask._id),
              $set("isTally" -> ask.isTally) ++ $set("isConcluded" -> ask.isConcluded)
            )
            .void
        else funit
      case None => insert(ask)
    }

  private def insert(ask: Ask): Funit = coll.insert.one(ask).void

  private def delete(id: Ask.ID): Funit = coll.delete.one($id(id)).void

  private def setUrl(ask: Ask, url: String): Funit =
    if (!ask.url.contains(url))
      coll.ext
        .findAndUpdate[Ask](
          selector = $id(ask._id),
          update = $set("url" -> url),
          fetchNewObject = false
        )
        .void
    else funit
}

object AskApi {

  /** class Updated aggregates return values from askApi.prepare
    *
    * @param text
    *   annotated form text - markup will contain ask ids for render() stage
    * @param cookie
    *   updated cookie
    */
  case class Updated(text: String, cookie: Option[Ask.Cookie])

  /** return provided text with any ask markup stripped
    * @param text
    *   to strip
    * @param n
    *   provide this to take(n)
    * @return
    *   n characters of stripped text
    */

  def stripAsks(text: String, n: Int = -1): String =
    matchAskRe.replaceAllIn(text, "").take(if (n == -1) text.length else n)

  // markdown fuckery - strip the backslashes when markdown escapes one of the characters below
  val stripMarkdownRe = raw"\\([*_`~.!{})(\[\]\-+|<>])".r // that might be overkill

  // markup
  private val matchAskRe = (
    raw"(?m)^\?\?\h*\S.*\R"         // match "?? <question>"
      + raw"(\?=.*\R)?"             // match optional "?= <params>"
      + raw"(\?[#@]\h*\S.*\R?){2,}" // match 2 or more "?# <choice>"
      + raw"(\?!.*\R?)?"            // match optional "?! <reveal>"
  ).r
  private val matchIdRe       = raw"(?m)^\?=.*askid:(\S{8})".r
  private val matchQuestionRe = raw"(?m)^\?\?(.*)".r
  private val matchParamsRe   = raw"(?m)^\?=(.*)".r
  private val matchChoicesRe  = raw"(?m)^\?[#@](.*)".r
  private val matchAnswerRe   = raw"(?m)^\?@(.*)".r
  private val matchRevealRe   = raw"(?m)^\?!(.*)".r

  // cookie
  private val v1Prefix     = "v1:"
  private val v1MatchIdsRe = raw"v1:\[([^]]+)]".r
  private val v1IdSep      = ","

  // renders ask as markup text
  private def askToText(ask: Ask): String = {
    val sb = new StringBuilder("")
    sb ++= s"?? ${ask.question}\n"
    sb ++= s"?= askid:${ask._id}"
    sb ++= s"${ask.isPublic ?? " public"}"
    sb ++= s"${ask.isTally ?? " tally"}"
    sb ++= s"${ask.isConcluded ?? " concluded"}\n"
    sb ++= ask.choices.map(c => s"?${if (ask.answer.contains(c)) "@" else "#"} $c\n").mkString
    (sb ++= s"${ask.reveal.fold("")(a => s"?! $a\n")}").toString
  }

  // assumes inputs are non-overlapping and sorted
  private def getIntervalClosure(subs: Seq[(Int, Int)], upper: Int): Seq[(Int, Int)] = {
    val points = (0 :: subs.toList.flatten(i => List(i._1, i._2)) ::: upper :: Nil).distinct
    points.zip(points.tail)
  }

  // returns the (begin, end) offsets of ask markups in text.
  private def getMarkupOffsets(text: String): Seq[(Int, Int)] =
    matchAskRe.findAllMatchIn(text).map(m => (m.start, m.end)).toList

  private def makeV1Cookie(idList: Seq[Ask.ID]): Option[String] =
    idList match {
      case Nil => None
      case ids => ids.mkString(s"$v1Prefix[", v1IdSep, "]").some
    }

  private def extractV1Cookie(cookie: Option[Ask.Cookie]): Option[String] =
    v1MatchIdsRe.findFirstMatchIn(~cookie).map(_.group(1))

  private def extractIds(cookie: Option[Ask.Cookie]): Seq[Ask.ID] =
    extractV1Cookie(cookie) match {
      case Some(m) => m.r.split(v1IdSep).toSeq
      case None    => Nil
    }

  private def extractQuestion(t: String): String =
    matchQuestionRe.findFirstMatchIn(t).get.group(1).trim

  private def extractParams(t: String): Option[String] =
    matchParamsRe.findFirstMatchIn(t).map(_.group(1).trim.toLowerCase)

  private def extractChoices(t: String): Ask.Choices =
    matchChoicesRe.findAllMatchIn(t).map(_.group(1).trim).distinct.toVector

  private def extractId(t: String): Option[Ask.ID] =
    matchIdRe.findFirstMatchIn(t).map(_.group(1))

  private def extractAnswer(t: String): Option[String] =
    matchAnswerRe.findFirstMatchIn(t).map(_.group(1).trim)

  private def extractReveal(t: String): Option[String] =
    matchRevealRe.findFirstMatchIn(t).map(_.group(1).trim)

  private def stripMarkdownEscapes(t: String): String = {
    stripMarkdownRe.replaceAllIn(t, "$1")
  }
}
