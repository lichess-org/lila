package lila.poll

import lila.user.User

/*
Example text from a message with valid poll markup:

Hey everyone, please let me know what works best for our weekly team bullet
tournament.
?? When should it be?
?* Mon 7:00pm GMT
?* Tue 6:30pm GMT
?* Wed 8:00pm GMT

Poll markup must be contiguous lines.
?? specifies the question.  it is required and client _should_ display it.
?= is optional - it can be followed by public and/or tally.  These result
in boolean flags that client code can interpret or ignore.  public
means it's OK to show who voted for what.  tally means it's OK to show
results before the poll is closed.  Both are false if ?= is omitted.
?* specifies a choice to vote on.  2 or more are required

the ??, ?=, ?* directives must be the first non-whitespace characters on a
new line. empty lines or text between markup lines are not allowed.
Multiline questions are not allowed
 */

case class Poll(
    _id: Poll.ID,
    question: String,
    choices: Poll.Choices,
    isPublic: Boolean, // open ballot
    isTally: Boolean,  // results visible before closing
    isClosed: Boolean, // no more voting allowed if true
    votes: Option[Poll.Votes],
    offset: Int // offset of markup within unmarked text
) {
  def isEquivalent(p: Poll): Boolean = // something important change?
    question == p.question &&
      choices.equals(p.choices) &&
      isPublic == p.isPublic &&
      isTally == p.isTally // if true, keep the old one and save the votes

  def copy(id: Option[Poll.ID] = None, offset: Option[Int] = None) =
    Poll(
      _id = id getOrElse _id,
      question = question,
      choices = choices,
      isPublic = isPublic,
      isTally = isTally,
      votes = votes,
      isClosed = isClosed,
      offset = offset getOrElse this.offset
    )
}

object Poll {
  type ID      = String
  type Magic   = String
  type Votes   = Map[User.ID, Int] // value = index in Choices list
  type Choices = List[String]
  type Results = Map[String, Int]  // key = choice, value = vote count
  val idSize = 8

  def make(question: String, choices: Choices, isPublic: Boolean, isTally: Boolean, offset: Int) =
    Poll(
      _id = lila.common.ThreadLocalRandom nextString idSize,
      question = question,
      choices = choices,
      isPublic = isPublic,
      isTally = isTally,
      votes = None,
      isClosed = false,
      offset = offset
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
