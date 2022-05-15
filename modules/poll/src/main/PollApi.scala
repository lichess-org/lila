package lila.poll

import scala.concurrent.Future
import lila.db.dsl._
import lila.base.LilaIterableFuture
import lila.notify.NotifyApi
import lila.user.User
import reactivemongo.api.bson._
import lila.poll.Poll.ImportMe._

final class PollApi(
    coll: Coll,
    notifyApi: NotifyApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import PollApi._

  implicit val PollBSONHandler = Macros.handler[Poll]

  /** Gets a poll object from the db
    * @return
    *   future containing poll, or None if not found
    * @param pid
    *   id of the poll object
    */
  def get(pid: Poll.ID): Fu[Option[Poll]] =
    coll.byId[Poll](pid)

  /** Submits a user vote in a poll, returns updated object
    * @return
    *   future containing updated poll, or None if not found
    * @param pid
    *   id of the poll object
    * @param uid
    *   id of the user
    * @param choice
    *   integer index into Poll.choices list
    */

  def vote(pid: Poll.ID, uid: User.ID, choice: Int): Fu[Option[Poll]] = {
    s"$uid $pid $choice".pp
    coll.ext.findAndUpdate[Poll](
      selector = $id(pid),
      update = {
        $set(s"votes.$uid" -> choice)
      },
      fetchNewObject = true
    )
  }

  /** Closes a poll and notifies all voting users that results available for viewing
    * @return
    *   future containing results of the poll, or None if not found
    * @param pid
    *   id of the poll object
    * @param href
    *   provide a uri for notifications (where results can be viewed)
    */
  def close(pid: Poll.ID, href: String): Fu[Option[Poll.Result]] =
    get(pid) flatMap {
      case Some(p) =>
        val result = Poll.Result(p)
        coll.update.one($id(pid), $set("isClosed" -> true))

        // notify all on poll closed

        fuccess(result.some)
      case None =>
        fufail(s"PollApi.close:  Poll id $pid not found")
    }

  /** deletes a user vote from its db object
    *
    * @return
    *   a future containing the updated poll, or None if it has been removed
    * @param pid
    *   id of the poll object
    * @param uid
    *   id of the user
    */

  def deleteVote(pid: Poll.ID, uid: User.ID): Fu[Option[Poll]] =
    coll.ext.findAndUpdate[Poll](
      selector = $id(pid),
      update = {
        $unset(s"votes.$uid")
      },
      fetchNewObject = true
    )

  def deletePolls(cookie: Option[Poll.Cookie]) = {
    extractIds(cookie) map { pid =>
      delete(pid)
    }
  }

  /** Aggregates return values from pollApi.prepare
    *
    * @param text
    *   annotated form text (any markup will get poll ids for render() stage
    * @param cookie
    *   updated cookie (internal use - right now just removing unused polls from db)
    */
  case class Updated(text: String, cookie: Option[Poll.Cookie])

  /** Updates markup in input text, updates cookie, and reconciles any poll markup with db. Call this before
    * creating your database object whenever a form is submitted.
    *
    * @return
    *   future tuple (annotatedText, Some(cookie)) or (formText, None) if no poll markup
    * @param formText
    *   annotate formText markup with ids and return annotatedText
    * @param cookie
    *   existing poll cookie or None
    */

  def prepare(formText: String, cookie: Option[Poll.Cookie] = None): Fu[Updated] = {
    "here we are!".pp
    val oText         = new StringBuilder
    val markupOffsets = getMarkupOffsets(formText)
    val (idList, opList) = getIntervalClosure(markupOffsets, formText.length).map { interval =>
      val txt = formText.slice(interval._1, interval._2)
      if (!markupOffsets.contains(interval)) {
        oText ++= txt
        (null, funit)
      } else {
        val params = extractParams(txt)
        val poll = Poll.make(
          question = extractQuestion(txt),
          choices = extractChoices(txt),
          isPublic = params.fold(false)(_.contains("public")),
          isTally = params.fold(false)(_.contains("tally"))
        )
        oText ++= pollToText(poll).pp
        (poll._id, update(poll, extractPid(txt)))
      }
    }.unzip
    extractIds(cookie).filterNot(idList contains) map { pid =>
      delete(pid)
    }
    Future.sequence(opList) inject {
      Updated(oText.toString(), makeV1Cookie(idList.filterNot(_ == null)))
    } // wait for db inserts to complete before calling render
  }

  /** Generates a sequence of poll objects and unmarked text segments for view construction. The
    * Render.Element in return value is just a type alias for Either declared in Poll.Render, isPoll and
    * isText alias Left/Right for more readable match expressions i.e { case isPoll(p) => ... }
    *
    * @param text
    *   annotated poll markup returned from pollApi.update
    * @return
    *   future sequence of Render.Element (either poll or text segment)
    */
  def render(text: String): Fu[Seq[Poll.RenderElement]] = {
    val markupOffsets = getMarkupOffsets(text)
    val opList = getIntervalClosure(markupOffsets, text.length) map { interval =>
      val txt = text.slice(interval._1, interval._2)
      if (!markupOffsets.contains(interval))
        fuccess(isText(txt))
      else {
        extractPid(txt) match {
          case None =>
            throw new IllegalArgumentException(s"pollApi.render called with bad markup.")
          case Some(pid) =>
            get(pid) flatMap {
              case None =>
                throw new RuntimeException(s"pollApi.render pid $pid not found")
              case Some(p) =>
                fuccess(isPoll(p))
            }
        }
      }
    }
    Future.sequence(opList) flatMap { segments =>
      fuccess(segments)
    }
  }

  private def update(poll: Poll, id: Option[Poll.ID]) =
    id match {
      case None => insert(poll)
      case Some(pid) =>
        get(pid) flatMap {
          case None => // just insert(poll) here and pretend nothing's wrong?
            throw new RuntimeException(s"pollApi.update pid $pid not found")
          case Some(p) =>
            delete(p._id)
            if (p.isEquivalent(poll)) {
              insert(p.copy(_id = poll._id)) // preserve votes
            } else insert(poll)
        }
    }

  // renders poll as markup text
  private def pollToText(poll: Poll): String = (
    s"?? ${poll.question}\n"
      + s"?= pid:${poll._id}${poll.isPublic ?? " public"}${poll.isTally ?? " tally"}\n"
      + s"${poll.choices.mkString("?* ", "\n?* ", "\n")}"
  )

  private def insert(poll: Poll) =
    coll.insert.one(poll).void

  private def delete(pid: Poll.ID) =
    coll.delete.one($id(pid)).void

  // assumes inputs are non-overlapping and sorted
  private def getIntervalClosure(subs: Seq[(Int, Int)], upper: Int): Seq[(Int, Int)] = {
    val points = (0 :: subs.toList.flatten(i => List(i._1, i._2)) ::: upper :: Nil).distinct
    points.zip(points.tail)
  }

  // returns the (begin, end) offsets of poll markups in text.
  private def getMarkupOffsets(text: String): Seq[(Int, Int)] =
    matchPollRe.findAllMatchIn(text).map(m => (m.start, m.end)).toList

  private def makeV1Cookie(idList: Seq[Poll.ID]): Option[String] =
    idList match {
      case Nil => None
      case ids => ids.mkString(s"$v1Prefix[", v1IdSep, "]").some
    }

  private def extractV1Cookie(cookie: Option[Poll.Cookie]): Option[String] =
    v1MatchIdsRe.findFirstMatchIn(~cookie) match {
      case Some(m) => Some(m.group(1))
      case None    => None
    }

  private def extractIds(cookie: Option[Poll.Cookie]): Seq[Poll.ID] =
    extractV1Cookie(cookie) match {
      case Some(m) => m.r.split(v1IdSep).toSeq
      case None    => Nil
    }

  private def extractQuestion(pt: String): String =
    matchQuestionRe.findFirstMatchIn(pt).get.group(1).trim

  private def extractParams(pt: String): Option[String] =
    matchParamsRe.findFirstMatchIn(pt).map(_.group(1).trim.toLowerCase)

  private def extractChoices(pt: String): Seq[String] =
    matchChoicesRe.findAllMatchIn(pt).map(_.group(1).trim).toList

  private def extractPid(pt: String): Option[Poll.ID] =
    matchPidRe.findFirstMatchIn(pt).map(_.group(1))
}

object PollApi {
  // poll markup processing
  private val matchPollRe = (
    raw"(?m)^\?\?\h*\S.*\R"       // match "?? <question>"
      + raw"(\?=.*\R)?"           // match optional "?= <params>"
      + raw"(\?\*\h*\S.*\R?){2,}" // match 2 or more "/* <choice>"
  ).r
  private val matchPidRe      = raw"(?m)^\?=.*pid:(\S{8})".r
  private val matchQuestionRe = raw"(?m)^\?\?(.*)".r
  private val matchParamsRe   = raw"(?m)^\?=(.*)".r
  private val matchChoicesRe  = raw"(?m)^\?\*(.*)".r

  // cookie processing
  private val v1Prefix     = "v1:"
  private val v1MatchIdsRe = raw"v1:\[([^\]]+)\]".r
  private val v1IdSep      = ","
}
