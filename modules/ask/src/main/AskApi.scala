package lila.ask

import scala.concurrent.Future
import reactivemongo.api.bson._
import lila.db.dsl._
import lila.hub.actors.Timeline
import lila.user.User
import lila.ask.Ask.imports._
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
    * @param choice
    *   integer index into Ask.choices list, or -1 to remove user pick
    */

  def pick(id: Ask.ID, uid: User.ID, choice: Int): Fu[Option[Ask]] =
    coll.ext.findAndUpdate[Ask](
      selector = $id(id),
      update = {
        if (choice < 0) $unset(s"picks.$uid") else $set(s"picks.$uid" -> choice)
      },
      fetchNewObject = true
    )

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
    get(id) flatMap {
      case Some(ask) =>
        coll.update.one($id(id), $set("isConcluded" -> true))
        timeline ! Propagate(AskConcluded(ask.creator, ask.question, ~ask.url))
          .toUsers(ask.participants.toList)
          .exceptUser(ask.creator)
        fuccess(ask.some)
      case None =>
        fufail(s"AskApi.finish:  Ask id $id not found")
    }

  /** Resets the picks on an ask
    * @param id
    *   the ask to reset
    * @return
    *   ask with no picks
    */

  def reset(ask: Ask): Fu[Option[Ask]] = reset(ask._id)
  def reset(id: Ask.ID): Fu[Option[Ask]] =
    coll.ext.findAndUpdate[Ask](
      selector = $id(id),
      update = $unset("picks"),
      fetchNewObject = true
    )

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
          val sb = new StringBuilder("")
          ask.getPick(uid) map { i =>
            val choice = ask.choices(i)
            sb ++= s"${ask.question}:  $choice"
            ask.answer match {
              case Some(ans) if ans != choice => sb ++= s" ($ans)"
              case _                          => None
            }
          }
          sb.toString
        }
      )

  /** Deletes all asks associated with a cookie
    *
    * @param cookie
    */

  def deleteAsks(cookie: Option[Ask.Cookie]): Funit =
    coll.delete.one($inIds(extractIds(cookie))).void

  /** Updates markup in input text, updates cookie, and reconciles any ask markup with db. Call this before
    * creating your database object whenever a form is submitted. Caller must ensure this future completes
    * before calling render
    *
    * @return
    *   future Updated(annotatedText, Some(cookie)) or (formText, None) if no ask markup
    * @param formText
    *   annotate formText markup (if any) with ids and return annotatedText
    * @param creator
    *   the user id who posted this formText
    * @param cookie
    *   existing ask cookie or None
    * @param url
    *   None or Some(url) where ask can be viewed. If not known when prepare is called, url can be set later
    *   in render or at another time via setUrl(cookie)
    */
  def prepare(
      formText: String,
      creator: User.ID,
      cookie: Option[Ask.Cookie] = None,
      url: Option[String] = None
  ): Fu[Updated] = {
    formText.pp
    val annotated     = new StringBuilder
    val markupOffsets = getMarkupOffsets(formText)
    // val star          = if (isMarkdown(formText)) "\\*" else "*"
    val (idList, opList) = getIntervalClosure(markupOffsets, formText.length).map { interval =>
      val txt = formText.slice(interval._1, interval._2)
      if (!markupOffsets.contains(interval)) {
        annotated ++= txt
        ("", funit)
      } else {
        val params = extractParams(txt)
        val ask = Ask.make(
          _id = extractId(txt),
          question = extractQuestion(txt),
          choices = extractChoices(txt),
          isPublic = params.fold(false)(_.contains("public")),
          isTally = params.fold(false)(_.contains("tally")),
          creator = creator,
          answer = extractAnswer(txt),
          reveal = extractReveal(txt),
          url = url
        )
        ask.pp(" in the blely")
        annotated ++= askToText(ask).pp
        (ask._id, update(ask))
      }
    }.unzip
    extractIds(cookie).filterNot(idList contains) map delete

    Future.sequence(opList) inject
      Updated(annotated.toString.pp, makeV1Cookie(idList filter (_.nonEmpty)))
  }

  /** Generates a sequence of ask objects and unmarked text segments for view construction. RenderElement is
    * alias for Either[Ask, String], and isAsk/isText alias Left/Right for clearer match expressions
    *
    * { case isAsk(e) => ... case isText(e) => }
    *
    * @param text
    *   annotated text provided by askApi.update
    * @param url
    *   if there is a redirect url and it was not provided in prepare, it is recommended you pass it here
    *   rather than separately in setUrl (to minimize database load)
    * @return
    *   future sequence of RenderElement (either ask or text)
    */
  def render(text: String, url: Option[String]): Fu[Seq[Ask.RenderElement]] = {
    val markupOffsets = getMarkupOffsets(text)
    "here we are in render".pp
    val opList = getIntervalClosure(markupOffsets, text.length) map { interval =>
      val txt = text.slice(interval._1, interval._2)
      if (!markupOffsets.contains(interval))
        fuccess(isText(txt))
      else {
        "shoiuld have one".pp
        extractId(txt) match {
          case None =>
            throw new IllegalArgumentException(s"askApi.render called with bad markup.")
          case Some(id) =>
            get(id) flatMap {
              case None =>
                throw new RuntimeException(s"askApi.render id $id not found")
              case Some(ask) =>
                url.map(setUrl(ask, _))
                ask.toString.pp("storing")
                fuccess(isAsk(ask))
            }
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
    coll.update
      .one(
        $inIds(extractIds(cookie)),
        $set("url" -> url),
        multi = true
      )
      .void

  // should probably do the below with db selectors
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
      case None =>
        insert(ask)
    }

  private def insert(ask: Ask): Funit =
    coll.insert.one(ask).void

  private def delete(id: Ask.ID): Funit =
    coll.delete.one($id(id)).void

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

  /** Updated aggregates return values from askApi.prepare
    *
    * @param text
    *   annotated form text - markup will contain ask ids for render() stage
    * @param cookie
    *   updated cookie
    */
  case class Updated(text: String, cookie: Option[Ask.Cookie])

  // markdown fuckery
  private val star         = ","                         // raw"(?:\*|\\\*)"
  private val isMarkdownRe = raw"(?m)^\?\\\*.*\R?{2,}".r // enough to tell

  // markup
  private val matchAskRe = (
    raw"(?m)^\?\?\h*\S.*\R"               // match "?? <question>"
      + raw"(\?=.*\R)?"                   // match optional "?= <params>"
      + raw"(\?$star{1,2}\h*\S.*\R?){2,}" // match 2 or more "/* <choice>"
      + raw"(\?!.*\R?)?"                  // match optional "?! <reveal>"
  ).r
  private val matchIdRe       = raw"(?m)^\?=.*id:(\S{8})".r
  private val matchQuestionRe = raw"(?m)^\?\?(.*)".r
  private val matchParamsRe   = raw"(?m)^\?=(.*)".r
  private val matchChoicesRe  = raw"(?m)^\?$star$star?(.*)".r
  private val matchAnswerRe   = raw"(?m)^\?$star$star(.*)".r
  private val matchRevealRe   = raw"(?m)^\?\!(.*)".r

  // cookie
  private val v1Prefix     = "v1:"
  private val v1MatchIdsRe = raw"v1:\[([^\]]+)\]".r
  private val v1IdSep      = ","

  // renders ask as markup text
  private def askToText(ask: Ask): String = (
    s"?? ${ask.question}\n"
      + s"?=${ask.isPublic ?? " public"}"
      + s"${ask.isTally ?? " tally"} "
      + s"${ask.isConcluded ?? " concluded"}"
      + s"id:${ask._id}\n"
      + ask.choices.map(c => s"?$star${ask.answer.contains(c) ?? star} $c\n").mkString
      + s"${ask.reveal.fold("")(e => s"?! $e\n")}"
  )

  // assumes inputs are non-overlapping and sorted
  private def getIntervalClosure(subs: Seq[(Int, Int)], upper: Int): Seq[(Int, Int)] = {
    val points = (0 :: subs.toList.flatten(i => List(i._1, i._2)) ::: upper :: Nil).distinct
    points.zip(points.tail)
  }

  // returns the (begin, end) offsets of ask markups in text.
  private def getMarkupOffsets(text: String): Seq[(Int, Int)] =
    matchAskRe.findAllMatchIn(text).map(m => (m.start, m.end)).toList

  // markdown escapes our asterisks, so we emit the same
  private def isMarkdown(text: String): Boolean = isMarkdownRe.matches(text)

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

  private def extractQuestion(pt: String): String =
    matchQuestionRe.findFirstMatchIn(pt).get.group(1).trim

  private def extractParams(pt: String): Option[String] =
    matchParamsRe.findFirstMatchIn(pt).map(_.group(1).trim.toLowerCase)

  private def extractChoices(pt: String): Ask.Choices =
    matchChoicesRe.findAllMatchIn(pt).map(_.group(1).trim).distinct.toVector

  private def extractId(pt: String): Option[Ask.ID] =
    matchIdRe.findFirstMatchIn(pt).map(_.group(1))

  private def extractAnswer(pt: String): Option[String] =
    matchAnswerRe.findFirstMatchIn(pt).map(_.group(1).trim)

  private def extractReveal(pt: String): Option[String] =
    matchRevealRe.findFirstMatchIn(pt).map(_.group(1).trim)

}
