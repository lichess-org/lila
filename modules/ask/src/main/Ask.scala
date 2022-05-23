package lila.ask

import lila.user.User
import org.joda.time.DateTime

/*
Example text from a message with valid ask markup:

?? When should we schedule next week's team tournament?
?= public tally
?# Mon 7:00pm GMT
?# Tue 6:30pm GMT
?# Wed 8:00pm GMT
?! Thanks for your input!

All markup for a given ask must occur on contiguous lines.  There are 2
types of ask, quizzes and polls.  A quiz has a special choice denoted by
?@ that indicates the correct answer (which is, of course, also a choice).
A quiz gives immediate feedback to the user when they make a selection.
Polls typically don't reveal results until they are concluded by their
creator, at which time all participants receive notification in their
timeline and a bar graph is shown at the poll location with the vote tallies.

?? specifies the question.  it is required and client should display it.

?= is optional and customizes the ask's behavior - ?= can be followed by
the keywords "public" and/or "tally".  These result
in boolean flags that client code can interpret or ignore.  public
means it's OK to show who voted for what.  tally means it's OK to show
results before the ask is closed.  Both are false if ?= is omitted.

?# specifies a choice

?@ specifies the correct answer.  the answer is also a choice

?! is the reveal text.  This is hidden from the user until they make a
selection.  It is intended for explanations of the answer in quiz asks but
it can be used for polls as well.

the ??, ?=, ?#/?@, ?! directives must be the first characters on their own
lines and they must be specified in order (aside from ?# with respect to ?@).
Empty lines or text between markup lines are not allowed.  Multiline
questions and reveals are not allowed, but there's no real limit on the
amount of text you can provide.

In the code, care is taken to only use "choice" when talking about the set
of valid selections whereas the term "pick" is always used when talking
about a user/choice pairing.
 */
case class Ask(
    _id: Ask.ID,
    question: String,
    choices: Ask.Choices,
    isPublic: Boolean,    // users picks are not secret
    isTally: Boolean,     // results visible before closing
    isConcluded: Boolean, // no more picks
    creator: User.ID,
    createdAt: DateTime,
    answer: Option[String],
    reveal: Option[String],
    picks: Option[Ask.Picks],
    url: Option[String]
) {

  /** Returns true if markup fields have been modified that invalide previous picks
    *
    * Only tally, concluded, and ?! reveal text can be changed
    *
    * @param p
    * @return
    *   vote-altering markup fields differ
    */

  def invalidatedBy(p: Ask): Boolean =
    question != p.question || choices != p.choices || isPublic != p.isPublic || answer != p.answer

  /** Gets a list of user ids who submitted picks
    * @return
    *   list of user ids
    */
  def participants: Seq[User.ID] =
    picks match {
      case Some(p) => p.keys.toSeq
      case None    => Nil
    }

  /** Does user have a pick for this ask?
    * @param uid,
    *   user id
    * @return
    *   true/false
    */
  def hasPick(uid: User.ID): Boolean = picks.exists(_.contains(uid))

  /** Gets user pick as an Option[Int]
    * @param uid,
    *   user id
    * @return
    *   pick value, if found
    */
  def getPick(uid: User.ID): Option[Int] =
    picks flatMap (p => p.get(uid).flatMap(v => Some(v)))

  /** number of users who picked choice
    * @param choice
    *   index or text within ask.choices
    * @return
    *   count
    */
  def count(choice: Int): Int    = picks.fold(0)(_.values.count(_ == choice))
  def count(choice: String): Int = count(choices.indexOf(choice))

  /** list users who picked choice
    * @param choice
    *   index or text within ask.choices
    * @return
    *   seq of user ids
    */
  def whoPicked(choice: String): Seq[User.ID] = whoPicked(choices.indexOf(choice))
  def whoPicked(choice: Int): Seq[User.ID] =
    picks match {
      case Some(p) =>
        p.collect { case (text, i) if choice == i => text }.toSeq
      case None => Nil
    }

  def isPoll: Boolean = answer.isEmpty

  def isQuiz: Boolean = answer.nonEmpty
}

object Ask {
  val idSize = 8

  type ID            = String
  type Cookie        = String
  type Choices       = IndexedSeq[String]
  type Picks         = Map[User.ID, Int] // _2 is index in Choices list
  type RenderElement = Either[Ask, String]

  object imports { // for slightly cleaner view code, import lila.ask.Ask.imports._
    val isAsk  = Left  // match rndrElem { case isAsk(q) => ...
    val isText = Right //                  case isText(t) => ...
  }

  def make(
      _id: Option[String],
      question: String,
      choices: Choices,
      isPublic: Boolean,
      isTally: Boolean,
      isConcluded: Boolean,
      creator: User.ID,
      answer: Option[String],
      reveal: Option[String]
  ) =
    Ask(
      _id = _id.getOrElse(lila.common.ThreadLocalRandom nextString idSize),
      question = question,
      choices = choices,
      isPublic = isPublic,
      isTally = isTally,
      isConcluded = isConcluded,
      createdAt = DateTime.now(),
      creator = creator,
      answer = answer,
      reveal = reveal,
      picks = None,
      url = None
    )
}
