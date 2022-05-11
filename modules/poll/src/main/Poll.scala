package lila.poll

/** poll markup must be contiguous, each line contains ?? question (required), ?= type params (optional), ?*
  * choice (2 or more required) in that order. the ??, ?=, ?* directives must be the first non-whitespace
  * characters on a new line. non-whitespace characters between markup lines are not allowed.
  *
  * Example text from post with valid poll markup:
  *
  * Hey everyone, please let me know what works best for you to schedule our weekly team bullet tournament.
  *
  * ?? Should it be... ?* Mon 7:00pm GMT ?* Tue 6:30pm GMT ?* Wed 8:00pm GMT
  */

case class Poll(
    _id: Poll.ID,
    question: String,
    choices: Poll.Choices,
    isPublic: Boolean, // open ballot
    isTally: Boolean,  // results visible before closing
    isClosed: false,   // no more voting allowed if true
    votes: Option[Poll.Votes]
)

object Poll {
  type ID      = String
  type Votes   = Map[String, Int] // value = index in PollChoices
  type Choices = List[String]
  type Results = Map[String, Int] // key = choice, value = vote count

  val idSize = 8

  def make(question: String, choices: Choices, isPublic: Boolean, isTally: Boolean) =
    Poll(
      _id = lila.common.ThreadLocalRandom nextString idSize,
      question = question,
      choices = choices,
      isPublic = isPublic,
      isTally = isTally,
      votes = None,
      isClosed = false
    )

  case class Result(
      question: String,
      counts: Results,
      votes: Option[Poll.Votes]
  )

  object Result {
    def apply(p: Poll): Result =
      Result(
        question = p.question,
        counts = p.choices
          .map(choice =>
            p.votes match {
              case Some(votes) =>
                val index = p.choices.indexOf(choice)
                (choice, votes.values.count(_ == index))
              case None => (choice, 0)
            }
          )
          .toMap,
        votes = p.isPublic ?? p.votes
      )
  }

  // returns the (begin, end) offsets of poll markups in text.
  def getOffsets(text: String): List[(Int, Int)] = (
    raw"(?m)^[ \t]*\?\?\s*[^\s].*\s+" + // match "?? <question>"
      raw"(^[ \t]*\?=.*\s+)?" +         // match optional "?= <public|tally>"
      raw"(^[ \t]*\?\*\s*[^\s].*\s*){2,}"
  ).r.findAllMatchIn(text).map(m => (m.start, m.end)).toList

  // returns the offset of the first poll markup in text
  def getOffset(text: String): Option[Int] =
    getOffsets(text) match {
      case first :: _ => Option(first._1)
      case _          => None
    }

  def textHasPoll(text: String): Boolean = getOffset(text).isDefined

  // returns (textMinusPolls, list[(newPoll, offsetInTextMinusPolls)]
  def extractAll(text: String): (String, List[(Int, Poll)]) = {
    var markupCursor: Int = 0
    val offsets           = getOffsets(text)
    val pollsAndOffsets = offsets.map(offset => {
      val (start, end) = offset
      val pt           = text.substring(start, end)

      // text validated by getOffsets regex. ok to call .get on required fields
      val question     = raw"(?m)^[ \t]*\?\?(.*)".r.findFirstMatchIn(pt).get.group(1).trim
      val modifiers    = raw"(?m)^[ \t]*\?=(.*)".r.findFirstMatchIn(pt).map(_.group(1).toLowerCase)
      val matchChoices = raw"(?m)^[ \t]*\?\*(.*)".r.findAllMatchIn(pt)

      val poll = Poll.make(
        question = question,
        choices = matchChoices.map(_.group(1).trim).toList,
        isPublic = modifiers.fold(false)(_.contains("public")),
        isTally = modifiers.fold(false)(_.contains("tally"))
      )
      markupCursor += end - start
      (end - markupCursor, poll)
    })
    // now we reassemble text minus markup
    markupCursor = 0 // i see no other way.  procedural dinosaur
    val newText: String = offsets
      .map(o => {
        val snippet = text.substring(markupCursor, o._1)
        markupCursor = o._2
        snippet
      })
      .mkString + (offsets match {
      case _ :+ last => text.substring(last._2)
      case Nil       => text
    })
    (newText, pollsAndOffsets)
  }

  // extracts the first poll in text, returns (textBeforePoll, poll)
  def extractOne(text: String): (String, Option[Poll]) =
    extractAll(text)._2 match {
      case first :: _ => (text.substring(0, first._1), Option(first._2))
      case Nil        => (text, None)
    }

  // renders poll as markup text
  def pollToText(poll: Poll): String = (
    s"?? ${poll.question}\n"
      + s"?=${if (poll.isPublic) " public"}${if (poll.isTally) " tally"}\n"
      + s"${poll.choices.mkString("?* ", "\n?* ", "\n")}"
  )
}
