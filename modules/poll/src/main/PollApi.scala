package lila.poll

import scala.concurrent.Future
import lila.db.dsl._
import lila.notify.NotifyApi
import lila.user.User
import reactivemongo.api.bson._

final class PollApi(
    coll: Coll,
    notifyApi: NotifyApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit val PollBSONHandler = Macros.handler[Poll]

  def get(pid: Poll.ID): Fu[Option[Poll]] =
    coll.byId[Poll](pid)

  def vote(pid: Poll.ID, uid: User.ID, choice: Int): Fu[Option[Poll]] =
    coll.ext.findAndUpdate[Poll](
      selector = $id(pid),
      update = {
        $set(s"votes.$uid" -> choice)
      },
      fetchNewObject = true
    )

  def close(pid: Poll.ID, href: String): Fu[Option[Poll.Result]] =
    get(pid) flatMap { case Some(p) =>
      // notify all on poll closed
      val result = Poll.Result(p)
      coll.update.one($id(pid), $set("isClosed" -> true)) inject result.some
    }

  def insert(poll: Poll) =
    coll.insert.one(poll).void

  // call this function after a user edit so that oldPid is overwritten in database
  // iff there has been a change to poll fields that would invalidate existing votes
  // or call it with origPid = None just to save a poll
  def saveIfModified(poll: Poll, origPid: Option[Poll.ID] = None): Fu[Poll] =
    origPid match {
      case None => fuccess(poll)
      case Some(pid) =>
        val newPoll = poll.copy(origPid)
        get(pid) flatMap {
          case Some(p) =>
            if (p.isEquivalent(poll)) fuccess(p)
            else {
              coll.update.one($id(pid), newPoll)
              fuccess(newPoll)
            }
          case None => // how does this happen?
            coll.update.one($id(pid), newPoll)
            fuccess(newPoll)
        }
    }

  def remove(pid: Poll.ID) =
    coll.delete.one($id(pid)).void

  def eraseVote(pid: Poll.ID, uid: User.ID): Fu[Option[Poll]] =
    coll.ext.findAndUpdate[Poll](
      selector = $id(pid),
      update = {
        $unset(s"votes.$uid")
      },
      fetchNewObject = true
    )
  /*// extracts the first poll in text, returns (textPriorToPoll, poll)
  def extractOne(text: String): (String, Option[Poll]) =
    extractAll(text)._2 match {
      case first :: _ => (text.substring(0, first._1), Option(first._2))
      case Nil        => (text, None)
    }
   */
  def hasPoll(text: String): Boolean = getOffset(text).isDefined

  // returns the offset of the first poll markup in text
  def getOffset(text: String): Option[Int] =
    getOffsets(text) match {
      case first :: _ => Option(first._1)
      case _          => None
    }

  val matchPollRe = (
    raw"(?m)^\h*\?\?\h*\S.*\R"       // match "?? <question>"
      + raw"(\h*\?=.*\R)?"           // match optional "?= <public|tally>"
      + raw"(\h*\?\*\h*\S.*\R?){2,}" // match 2 or more "?* <choice>"
  ).r

  // returns the (begin, end) offsets of poll markups in text.
  def getOffsets(text: String): List[(Int, Int)] =
    matchPollRe.findAllMatchIn(text).map(m => (m.start, m.end)).toList

  // returns marked up text to be presented when editing existing item
  def getEditable(unmarked: String, magic: Poll.Magic): Fu[String] = {
    parseMagic(magic) map { polls =>
      val lastSegment = polls match {
        case Nil    => unmarked
        case _ :+ p => unmarked.substring(p.offset, unmarked.length)
      }
      ((0 :: polls.map(_.offset)) zip polls)
        .map(t => unmarked.substring(t._1, t._2.offset) + pollToText(t._2))
        .mkString("", "", lastSegment)
    }
  }

  // just an aggregator for extractAll's results
  case class ExtractionResults(
      reducedText: String,
      polls: List[Poll],
      magic: Poll.Magic
  )
  def extractAll(text: String): Fu[ExtractionResults] = {
    var totalMarkup: Int = 0

    val offsets = getOffsets(text)
    val listOfFutures = offsets map { offset =>
      val (start, end) = offset
      totalMarkup += end - start

      val pt     = text.substring(start, end)
      val params = extractParams(pt)
      val poll = Poll.make( // prepare a brand new poll, might need it
        question = extractQuestion(pt),
        choices = extractChoices(pt),
        isPublic = params.fold(false)(_.contains("public")),
        isTally = params.fold(false)(_.contains("tally")),
        offset = end - totalMarkup
      )
      extractPid(pt) match {
        case None =>
          fuccess(poll)
        case Some(pid) =>
          get(pid) flatMap {
            case None    => fuccess(poll)
            case Some(p) => fuccess(p.copy(offset = Some(end - totalMarkup)))
          }
      }
    }
    Future.sequence(listOfFutures) map { polls =>
      ExtractionResults(matchPollRe.replaceAllIn(text, ""), polls, polls.map(_._id).mkString(","))
    }
  }

  // renders poll as markup text
  private def pollToText(poll: Poll): String = (
    s"?? ${poll.question}\n"
      + s"?= pid(${poll._id})${poll.isPublic ?? " public"}${poll.isTally ?? " tally"}\n"
      + s"${poll.choices.mkString("?* ", "\n?* ", "\n")}"
  )

  private def parseMagic(magic: Poll.Magic): Fu[List[Poll]] =
    coll.byIds(magic.r.split(","))

  private def extractQuestion(pt: String): String =
    raw"(?m)^\h*\?\?(.*)".r.findFirstMatchIn(pt).get.group(1).trim

  private def extractParams(pt: String): Option[String] =
    raw"(?m)^\h*\?=(.*)".r.findFirstMatchIn(pt).map(_.group(1).trim.toLowerCase)

  private def extractChoices(pt: String): List[String] =
    raw"(?m)^\h*\?\*(.*)".r.findAllMatchIn(pt).map(_.group(1).trim).toList

  private def extractPid(pt: String): Option[Poll.ID] =
    raw"(?m)^\h*\?=.*pid\((\S{8})\)".r.findFirstMatchIn(pt).map(_.group(1))
}

//def remove(notifies: Notification.Notifies, selector: Bdoc): Funit =
//coll.delete.one(userNotificationsQuery(notifies) ++ selector).void

/*
def react(categSlug: String, postId: Post.ID, me: User, reaction: String, v: Boolean): Fu[Option[Post]] =
  Post.Reaction.set(reaction) ?? {
    if (v) lila.mon.forum.reaction(reaction).increment()
    postRepo.coll.ext
      .findAndUpdate[Post](
        selector = $id(postId) ++ $doc("categId" -> categSlug, "userId" $ne me.id),
        update = {
          if (v) $addToSet(s"reactions.$reaction" -> me.id)
          else $pull(s"reactions.$reaction"       -> me.id)
        },
        fetchNewObject = true
      )
  }
 */
