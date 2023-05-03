package lila.ask

import lila.user.User

case class Ask(
    _id: Ask.ID,
    question: String,
    choices: Ask.Choices,
    tags: Ask.Tags,
    creator: UserId,
    createdAt: java.time.Instant,
    answer: Option[String], // correct answer, if defined then this ask is a quiz
    footer: Option[String], // reveal text for quizzes or optional text prompt for feedbacks
    picks: Option[Ask.Picks],
    feedback: Option[Ask.Feedback],
    url: Option[String]
) {
  // changes to any of the fields checked in compatible will invalidate votes and feedback
  def compatible(a: Ask): Boolean =
    question == a.question &&
      choices == a.choices &&
      answer == a.answer &&
      footer == a.footer &&
      creator == a.creator &&
      isPublic == a.isPublic &&
      isAnon == a.isAnon &&
      isRanked == a.isRanked &&
      isMulti == a.isMulti &&
      isRange == a.isRange

  def merge(dbAsk: Ask): Ask =
    if (this.compatible(dbAsk)) // keep votes & feedback
      if (tags equals dbAsk.tags) dbAsk
      else dbAsk.copy(tags = tags)
    else copy(url = dbAsk.url) // discard votes & feedback

  def participants: Seq[String] =
    picks match {
      case Some(p) => p.keys.toSeq
      case None    => Nil
    }

  def isAnon: Boolean      = tags.exists(_ startsWith "anon") // hide voters from creator/mods
  def isPublic: Boolean    = tags contains "public"           // everyone can see who voted for what
  def isTally: Boolean     = tags contains "tally"            // partial results viewable before conclusion
  def isConcluded: Boolean = tags contains "concluded"        // no more votes, notify participants

  def isRandom: Boolean   = tags contains "random"              // randomize order of choices
  def isCenter: Boolean   = tags contains "center"              // horizontally center each row of choices
  def isVertical: Boolean = tags.exists(_ startsWith "vert")    // one choice per row
  def isStretch: Boolean  = tags.exists(_ startsWith "stretch") // stretch choices to fill width

  def isMulti: Boolean    = tags.exists(_ startsWith "multi") // multiple choices allowed
  def isRanked: Boolean   = tags.exists(_ startsWith "rank")  // drag to sort
  def isRange: Boolean    = tags.exists(_ startsWith "range") // slider
  def isFeedback: Boolean = tags contains "feedback"          // has a feedback/submit form
  def isQuiz: Boolean     = answer nonEmpty                   // has a correct answer

  def range: Int = tags.find(_ startsWith "range(") flatMap (_.drop(6).toIntOption) getOrElse choices.size
  def anon(user: UserId): String = if isAnon then Ask.anon(user, _id) else user.value

  def hasPickFor(uid: UserId): Boolean               = picks exists (_ contains anon(uid))
  def hasPickFor(o: Option[User]): Boolean           = o.fold(false)(u => hasPickFor(u.id))
  def picksFor(uid: UserId): Option[Vector[Int]]     = picks flatMap (_ get anon(uid))
  def picksFor(o: Option[User]): Option[Vector[Int]] = o flatMap (u => picksFor(u.id))
  def firstPickFor(uid: UserId): Option[Int]         = picksFor(uid) flatMap (_ headOption)
  def firstPickFor(o: Option[User]): Option[Int]     = o flatMap (u => firstPickFor(u.id))

  def rankingFor(uid: UserId): Option[IndexedSeq[Int]] =
    picks flatMap (_ get anon(uid))

  def hasFeedbackFor(uid: UserId): Boolean     = feedback exists (_ contains anon(uid))
  def feedbackFor(uid: UserId): Option[String] = feedback flatMap (_ get anon(uid))

  def count(choice: Int): Int    = picks.fold(0)(_.values.count(_ contains choice))
  def count(choice: String): Int = count(choices indexOf choice)

  def whoPicked(choice: String): List[String] = whoPicked(choices indexOf choice)
  def whoPicked(choice: Int): List[String] =
    picks getOrElse (Nil) collect {
      case (uid, ls) if ls contains choice => uid
    } toList

  def whoPickedAt(choice: Int, rank: Int): List[String] =
    picks getOrElse (Nil) collect {
      case (uid, ls) if ls.indexOf(choice) == rank => uid
    } toList

  @inline private def constrain(index: Int) =
    index atMost (choices.size - 1) atLeast 0

  def totals: Vector[Int] =
    picks match {
      case Some(pmap) if choices.nonEmpty && pmap.nonEmpty =>
        val results = Array.ofDim[Int](choices.size)
        pmap.values.foreach(_ foreach { it => results(constrain(it)) += 1 })
        results.toVector
      case _ =>
        Vector.fill(choices.size)(0)
    }
  // index of returned vector maps to choices list, value is from [0f, choices.size-1f] where 0 is "best" rank
  def averageRank: Vector[Float] =
    picks match {
      case Some(pmap) if choices.nonEmpty && pmap.nonEmpty =>
        val results = Array.ofDim[Int](choices.size)
        pmap.values foreach { ranking =>
          for (it <- choices.indices)
            results(constrain(ranking(it))) += it
        }
        results.map(_ / pmap.size.toFloat).toVector
      case _ =>
        Vector.fill(choices.size)(0f)
    }
  // a square matrix M describing the response rankings. each element M[i][j] is the number of
  // respondents who preferred the choice i at rank j or below (effectively in the top j+1 picks)
  def rankMatrix: Array[Array[Int]] =
    picks match {
      case Some(pmap) if choices.nonEmpty && pmap.nonEmpty =>
        val n   = choices.size
        val mat = Array.ofDim[Int](n, n)
        pmap.values foreach { ranking =>
          for (i <- choices.indices)
            for (j <- choices.indices)
              mat(i)(j) += (if (ranking(i) <= j) 1 else 0)
        }
        mat
      case _ =>
        Array.ofDim[Int](0, 0)
    }
}

object Ask {
  val idSize = 8

  type ID       = String
  type Tags     = Set[String]
  type Choices  = Vector[String]
  type Picks    = Map[String, Vector[Int]] // ranked list of indices into Choices
  type Feedback = Map[String, String]

  def make(
      _id: Option[String],
      question: String,
      choices: Choices,
      tags: Tags,
      creator: UserId,
      answer: Option[String],
      footer: Option[String]
  ) =
    Ask(
      _id = _id getOrElse (ornicar.scalalib.ThreadLocalRandom nextString idSize),
      question = question,
      choices = choices,
      tags = tags,
      createdAt = java.time.Instant.now(),
      creator = creator,
      answer = answer,
      footer = footer,
      picks = None,
      feedback = None,
      url = None
    )
  private val base64 = java.util.Base64
    .getEncoder()
    .withoutPadding();

  def anon(uid: UserId, aid: Ask.ID): String =
    "anon-" + new String(base64.encode(com.roundeights.hasher.Algo.sha1(s"${uid.value}-$aid").bytes))
      .substring(0, 11) // 66 bits is plenty of entropy, collisions are fairly harmless

}
