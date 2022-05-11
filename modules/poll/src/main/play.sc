
case class Poll(
                 _id: Poll.ID,
                 question: String,
                 choices: Poll.Choices,
                 isPublic: Boolean, // open ballot
                 isTally: Boolean, // results visible before closing
                 votes: Option[Poll.Votes],
               )

object Poll {
  type ID = String
  type Votes = Map[String, Int] // value = index in PollChoices
  type Choices = List[String]
  type Results = Map[String, Int] // key = choice, value = vote count

  val idSize = 8

  def make(question: String, choices: Choices, isPublic: Boolean, isTally: Boolean) =
    Poll(
      _id = "",//lila.common.ThreadLocalRandom nextString idSize,
      question = question,
      choices = choices,
      isPublic = isPublic,
      isTally = isTally,
      votes = None
    )

  case class Result(
                     question: String,
                     counts: Results,
                     votes: Option[Poll.Votes],
                   )

  object Result {
    def apply(p: Poll): Result =
      Result(
        question = p.question,
        counts = p.choices.map(
          choice => p.votes match {
            case Some(votes) =>
              val index = p.choices.indexOf(choice) // just do this once
              (choice, votes.values.count(_ == index))
            case None => (choice, 0)
          }
        ).toMap,
        votes = if (p.isPublic) p.votes else None,
      )
  }

  // returns the (begin, end) offsets of poll markups in text.
  // poll markups must be contiguous and each poll must contain
  // ?? (required), ?= (optional), ?* (2 or more required) in
  // that order.  the ??, ?=, ?* directives must be the first
  // non-white space on a new
  // line.  non-whitespace between markup lines is not allowed.
  def getOffsets(text: String): List[(Int, Int)] = (
    raw"(?m)^[ \t]*\?\?\s*[^\s].*\s+" + // match "?? <question>"
      raw"(^[ \t]*\?=.*\s+)?" + // match optional "?= <public|tally>"
      raw"(^[ \t]*\?\*\s*[^\s].*\s*){2,}" // match 2+ "?* <choice>"
    ).r.findAllMatchIn(text).map(m => (m.start, m.end)).toList

  // returns the offset of the first poll markup in text
  def getOffset(text: String): Option[Int] =
    getOffsets(text) match {
      case first :: _ => Option(first._1)
      case _ => None
    }

  def textHasPoll(text: String): Boolean = getOffset(text).isDefined

  // returns (textMinusPolls, list[(newPoll, offsetInTextMinusPolls)]
  def extractAll(text: String): (String, List[(Int, Poll)]) = {
    var markupCursor: Int = 0
    val offsets = getOffsets(text)
    println(offsets)
    val pollsAndOffsets = offsets.map(
      offset => {
        val (start, end) = offset
        val pt = text.substring(start, end)
        println(pt)
        // text validated by getOffsets regex. ok to call .get on required fields
        val question = raw"(?m)^[ \t]*\?\?(.*)".r.findFirstMatchIn(pt).get.group(1).trim
        val modifiers = raw"(?m)^[ \t]*\?=(.*)".r.findFirstMatchIn(pt).map(_.group(1).toLowerCase)
        val matchChoices = raw"(?m)^[ \t]*\?\*(.*)".r.findAllMatchIn(pt)

        val poll = Poll.make(
          question = question,
          choices = matchChoices.map(_.group(1).trim).toList,
          isPublic = modifiers.fold(false)(_.contains("public")),
          isTally = modifiers.fold(false)(_.contains("tally"))
        )
        markupCursor += end - start
        (end - markupCursor, poll)
      }
    )
    // now we reassemble text minus markup
    markupCursor = 0 // i see no other way.  procedural dinosaur
    val newText: String = offsets.map(o => {
      val snippet = text.substring(markupCursor, o._1)
      markupCursor = o._2
      snippet
    }).mkString + (offsets match {
      case _ :+ last => text.substring(last._2)
      case Nil => text
    })
    (newText, pollsAndOffsets)
  }

  // extracts the first poll in text, returns (textWithoutPoll, poll)
  def extractOne(text: String): (String, Option[Poll]) =
    extractAll(text)._2 match {
      case first :: _ => (text.substring(0, first._1), Option(first._2))
      case Nil => (text, None)
    }

  // renders poll as poll markup text
  def pollToText(poll: Poll): String = (
    s"?? ${poll.question}\n"
      + s"?=${if (poll.isPublic) " public"}${if (poll.isTally) " tally"}\n"
      + s"${poll.choices.mkString("?* ", "\n?* ", "\n")}"
    )
}
val pollText = """
  heyo heyo
?? I wonder question?
?= pUBlic tALLY
?* aNSwer
?* heres a choice
?* heres another
bunch of crap
yada yada yada
crazy doodles
?? oooh heres another
?* but theres not enough here
nope that wasnt good
lets try another
?? question
?* answer1
?* answer2
hahah
now a malformed one
?? question
?=
  ?? blah
 ?* h
  ?* p
"""

Poll.extractAll(pollText)._1
Poll.pollToText(Poll.extractOne(pollText)._2.get)

