package lila.ask

import lila.user.User
import org.joda.time.DateTime

/*
Example text from a message with valid ask markup:

Hey everyone, please let me know what works best for our weekly team bullet
tournament.
?? When should it be?
?= public tally
?* Mon 7:00pm GMT
?* Tue 6:30pm GMT
?* Wed 8:00pm GMT

Ask markup must be contiguous lines.  Due to the rendering implementation,
it cannot be used within block level html tags - kinda like markdown.

?? specifies the question.  it is required and client should display it.

?= is optional - it can be followed by [public|tally|ask].  These result
in boolean flags that client code can interpret or ignore.  public
means it's OK to show who voted for what.  tally means it's OK to show
results before the ask is closed.  Both are false if ?= is omitted.

?* specifies a choice to vote on.  2 or more are required

the ??, ?=, ?* directives must be the first characters on a
new line. empty lines or text between markup lines are not allowed.
Multiline questions are not allowed.
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

  /** returns true if fields specified by markup are different that should invalide previous picks
    *
    * only tally, concluded, and ?! reveal can be changed
    *
    * @param p
    * @return
    *   vote-altering markup fields differ
    */

  def invalidatedBy(p: Ask): Boolean =
    question != p.question || choices != p.choices || isPublic != p.isPublic || answer != p.answer

  /** gets a list of user ids who submitted picks
    * @return
    *   list of user ids who participated in this ask
    */

  def participants: Seq[User.ID] =
    picks match {
      case Some(p) => p.keys.toSeq
      case None    => Nil
    }

  /** does uid have a pick for this ask
    * @param uid,
    *   user id
    * @return
    *   true/false
    */
  def hasPick(uid: User.ID): Boolean = picks.exists(_.contains(uid))

  /** gets pick for uid as an Option[Int]
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
}

object Ask {
  val idSize = 8

  type ID            = String
  type Cookie        = String
  type Choices       = IndexedSeq[String]
  type Picks         = Map[User.ID, Int] // _2 is index in Choices list
  type Results       = Map[String, Int]  // _1 is choiceText, _2 is pick count
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
      creator: User.ID,
      answer: Option[String],
      reveal: Option[String],
      url: Option[String]
  ) =
    Ask(
      _id = _id.getOrElse(lila.common.ThreadLocalRandom nextString idSize),
      question = question,
      choices = choices,
      isPublic = isPublic,
      isTally = isTally,
      isConcluded = false,
      createdAt = DateTime.now(),
      creator = creator,
      answer = answer,
      reveal = reveal,
      picks = None,
      url = url
    )
}
