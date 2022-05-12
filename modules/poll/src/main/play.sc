case class Poll(
    _id: Poll.ID,
    question: String,
    choices: Poll.Choices,
    isPublic: Boolean, // open ballot
    isTally: Boolean,  // results visible before closing
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
      _id = "", // lila.common.ThreadLocalRandom nextString idSize,
      question = question,
      choices = choices,
      isPublic = isPublic,
      isTally = isTally,
      votes = None
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
                val index = p.choices.indexOf(choice) // just do this once
                (choice, votes.values.count(_ == index))
              case None => (choice, 0)
            }
          )
          .toMap,
        votes = if (p.isPublic) p.votes else None
      )
  }

  val matchPollRe = (
    raw"(?m)^\h*\?\?\h*\S.*\R" // match "?? <question>"
      + raw"(\h*\?=.*\R)?"     // match optional "?= <public|tally>"
      + raw"(\h*\?\*\h*\S.*\R?){2,}"
  ).r

  // returns the (begin, end) offsets of poll markups in text.
  // poll markups must be contiguous and each poll must contain
  // ?? (required), ?= (optional), ?* (2 or more required) in
  // that order.  the ??, ?=, ?* directives must be the first
  // non-white space on a new
  // line.  non-whitespace between markup lines is not allowed.
  def getOffsets(text: String): List[(Int, Int)] =
    matchPollRe.findAllMatchIn(text).map(m => (m.start, m.end)).toList

  // returns the offset of the first poll markup in text
  def getOffset(text: String): Option[Int] =
    getOffsets(text) match {
      case first :: _ => Option(first._1)
      case _          => None
    }

  def textHasPoll(text: String): Boolean = getOffset(text).isDefined

  // returns (textMinusMarkup, list[(newPoll, offsetInTextMinusMarkup)]
  def extractAll(text: String): (String, List[(Int, Poll)]) = {
    var markupCursor: Int = 0
    val offsets           = getOffsets(text)
    println(offsets)
    val pollsAndOffsets = offsets.map(offset => {
      val (start, end) = offset
      val pt           = text.substring(start, end)

      // text validated by getOffsets regex. ok to call .get on required fields
      val question     = raw"(?m)^\h*\?\?(.*)".r.findFirstMatchIn(pt).get.group(1).trim
      val params       = raw"(?m)^\h*\?=(.*)".r.findFirstMatchIn(pt).map(_.group(1).trim)
      val matchChoices = raw"(?m)^\h*\?\*(.*)".r.findAllMatchIn(pt)
      val pid = params match { case Some(m) => raw"pid\((\S{8})\)".r.findFirstMatchIn(m).map(_.group(1)) }
      val poll = Poll.make(
        question = question,
        choices = matchChoices.map(_.group(1).trim).toList,
        isPublic = params.fold(false)(_.contains("public")),
        isTally = params.fold(false)(_.contains("tally"))
      )
      markupCursor += end - start // ask me about my "var life" tattoo
      (end - markupCursor, poll)
    })
    // now we reassemble text minus markup with some offset zippery
    val textMinusMarkup =
      (0 :: offsets.map(_._2) zip (offsets.map(_._1) ::: text.length :: Nil))
        .map(o => text.substring(o._1, o._2))
        .mkString
    (matchPollRe.replaceAllIn(text, ""), pollsAndOffsets)
  }

  // extracts the first poll in text, returns (textWithoutPoll, poll)
  def extractOne(text: String): (String, Option[Poll]) =
    extractAll(text)._2 match {
      case first :: _ => (text.substring(0, first._1), Option(first._2))
      case Nil        => (text, None)
    }

  // renders poll as poll markup text
  def pollToText(poll: Poll): String = (
    s"?? ${poll.question}\n"
      + s"?=${if (poll.isPublic) " public" else ""}${if (poll.isTally) " tally" else ""}\n"
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
?? here's some shit ?* on the same line
?= booga booga
?* here yago ?* there it went
?* there it goes

?? oooh heres another
?* but theres not enough here
nope that wasnt good
lets try another

  
?? question""" + "\n\n" + """?* answer1
?* answer2
hahah
now a malformed one
?? question
?=
  ?? blah""" + "\n\r" + """?* h
  ?* p
  and here's some more crap
  after it
?? do we get
?= pid(XXDDFFRR)
?* the
?* spaces after?"""

val res = Poll.extractAll(pollText)
/*
var insertionPoint = 0
res match {
  case (s, ls) =>
    ls map { case (off, p) =>
      // println(s"bloodah:  $insertionPoint, $off")
      print(s.substring(insertionPoint, off))
      print(Poll.pollToText(p))
      insertionPoint = off
    }
}
Poll.pollToText(Poll.extractOne(pollText)._2.get)
var indices = (List(0) :: List(3, 2, 1).map(p => List.fill(2)(p)) ::: List(-1) :: Nil).flatten
List(5) match { case blah :+ l => println(l) }
 */
