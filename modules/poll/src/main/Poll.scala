package lila.poll

import lila.user.User
import org.joda.time.DateTime

/*
Example text from a message with valid poll markup:

Hey everyone, please let me know what works best for our weekly team bullet
tournament.
?? When should it be?
?= public tally
?* Mon 7:00pm GMT
?* Tue 6:30pm GMT
?* Wed 8:00pm GMT

Poll markup must be contiguous lines.  Due to the rendering implementation,
it cannot be used within block level html tags - kinda like markdown.

?? specifies the question.  it is required and client should display it.

?= is optional - it can be followed by [public|tally|quiz].  These result
in boolean flags that client code can interpret or ignore.  public
means it's OK to show who voted for what.  tally means it's OK to show
results before the poll is closed.  Both are false if ?= is omitted.

?* specifies a choice to vote on.  2 or more are required

the ??, ?=, ?* directives must be the first characters on a
new line. empty lines or text between markup lines are not allowed.
Multiline questions are not allowed.
 */
case class Poll(
    _id: Poll.ID,
    question: String,
    choices: Poll.Choices,
    isPublic: Boolean, // open ballot
    isTally: Boolean,  // results visible before closing
    isClosed: Boolean, // no more voting allowed if true
    createdAt: DateTime,
    votes: Option[Poll.Votes]
) {
  def isEquivalent(p: Poll): Boolean =
    question == p.question &&
      choices.equals(p.choices) &&
      isPublic == p.isPublic &&
      isTally == p.isTally
}

object Poll {
  val idSize = 8

  type ID      = String
  type Choices = Seq[String]
  type Cookie  = String
  type Votes   = Map[User.ID, Int] // value = index in Choices list
  type Results = Map[String, Int]  // key = choice, value = vote count

  type RenderElement = Either[Poll, String]
  object ImportMe { // for cleaner view code import Poll.ImportMe._
    val isPoll = Left
    val isText = Right
  }

  def make(question: String, choices: Choices, isPublic: Boolean, isTally: Boolean, offset: Int = -1) =
    Poll(
      _id = lila.common.ThreadLocalRandom nextString idSize,
      question = question,
      choices = choices,
      isPublic = isPublic,
      isTally = isTally,
      isClosed = false,
      createdAt = DateTime.now(),
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
                val index = p.choices.indexOf(choice)
                (choice, votes.values.count(_ == index))
              case None => (choice, 0)
            }
          )
          .toMap,
        votes = p.isPublic ?? p.votes
      )
  }
}
