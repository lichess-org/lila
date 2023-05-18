package lila.ask

import lila.common.IpAddress
import lila.user.User

case class Ask(
    _id: Ask.ID,
    question: String,
    choices: Ask.Choices,
    tags: Ask.Tags,
    creator: UserId,
    createdAt: java.time.Instant,
    footer: Option[String], // optional text prompt for feedbacks
    picks: Option[Ask.Picks],
    feedback: Option[Ask.Feedback],
    url: Option[String]
):

  // changes to any of the fields checked in compatible will invalidate votes and feedback
  def compatible(a: Ask): Boolean =
    question == a.question &&
      choices == a.choices &&
      footer == a.footer &&
      creator == a.creator &&
      isOpen == a.isOpen &&
      isTraceable == a.isTraceable &&
      isAnon == a.isAnon &&
      isRanked == a.isRanked &&
      isMulti == a.isMulti

  def merge(dbAsk: Ask): Ask =
    if this.compatible(dbAsk) then // keep votes & feedback
      if tags equals dbAsk.tags then dbAsk
      else dbAsk.copy(tags = tags)
    else copy(url = dbAsk.url) // discard votes & feedback

  def participants: Seq[String] = picks match
    case Some(p) => p.keys.filter(!_.startsWith("anon-")).toSeq
    case None    => Nil

  lazy val isOpen      = tags contains "open"               // allow votes from anyone (no acct reqired)
  lazy val isAnon      = tags.exists(_ startsWith "anon")   // hide voters from creator/mods
  lazy val isTraceable = tags.exists(_ startsWith "trace")  // everyone can see who voted for what
  lazy val isTally     = tags contains "tally"              // partial results viewable before conclusion
  lazy val isConcluded = tags contains "concluded"          // closed poll
  lazy val isRandom    = tags.exists(_ startsWith "random") // randomize order of choices
  lazy val isMulti    = !isRanked && tags.exists(_ startsWith "multi") // multiple choices allowed
  lazy val isRanked   = tags.exists(_ startsWith "rank")               // drag to sort
  lazy val isFeedback = tags contains "feedback"                       // has a feedback/submit form
  // def isCenter = tags contains "center"             // horizontally center each row of choices
  lazy val isStretch  = tags.exists(_ startsWith "stretch") // stretch choices to fill width
  lazy val isCheckbox = !isRanked && isVertical             // use checkboxes, implies vertical
  lazy val isVertical = tags.exists(_ startsWith "vert")    // one choice per row

  // these accessors probably seem cumbersone
  // they were written to support app/views/ask.scala code
  def toAnon(user: UserId): Option[String] =
    (if isAnon then Ask.anonHash(user.value, _id) else user.value).some

  def toAnon(ip: IpAddress): Option[String] =
    isOpen option Ask.anonHash(ip.toString, _id)

  // NOTE - eid stands for effective id, either a user id or an anonymous hash
  def hasPickFor(o: Option[String]): Boolean =
    o ?? (eid => picks.exists(_ contains eid))

  def picksFor(o: Option[String]): Option[Vector[Int]] =
    o.flatMap(eid => picks.flatMap(_ get eid))

  def firstPickFor(o: Option[String]): Option[Int] =
    picksFor(o) flatMap (_ headOption)

  def hasFeedbackFor(o: Option[String]): Boolean =
    o ?? (eid => feedback.exists(_ contains eid))

  def feedbackFor(o: Option[String]): Option[String] =
    o ?? (eid => feedback flatMap (_ get eid))

  def count(choice: Int): Int    = picks.fold(0)(_.values.count(_ contains choice))
  def count(choice: String): Int = count(choices indexOf choice)

  def whoPicked(choice: String): List[String] = whoPicked(choices indexOf choice)
  def whoPicked(choice: Int): List[String] = picks getOrElse (Nil) collect {
    case (uid, ls) if ls contains choice => uid
  } toList

  def whoPickedAt(choice: Int, rank: Int): List[String] = picks getOrElse (Nil) collect {
    case (uid, ls) if ls.indexOf(choice) == rank => uid
  } toList

  @inline private def constrain(index: Int) = index atMost (choices.size - 1) atLeast 0

  def totals: Vector[Int] = picks match
    case Some(pmap) if choices.nonEmpty && pmap.nonEmpty =>
      val results = Array.ofDim[Int](choices.size)
      pmap.values.foreach(_.foreach { it => results(constrain(it)) += 1 })
      results.toVector
    case _ =>
      Vector.fill(choices.size)(0)

  // index of returned vector maps to choices list, value is from [0f, choices.size-1f] where 0 is "best" rank
  def averageRank: Vector[Float] = picks match
    case Some(pmap) if choices.nonEmpty && pmap.nonEmpty =>
      val results = Array.ofDim[Int](choices.size)
      pmap.values.foreach: ranking =>
        for (it <- choices.indices)
          results(constrain(ranking(it))) += it
      results.map(_ / pmap.size.toFloat).toVector
    case _ =>
      Vector.fill(choices.size)(0f)

  // a square matrix M describing the response rankings. each element M[i][j] is the number of
  // respondents who preferred the choice i at rank j or below (effectively in the top j+1 picks)
  def rankMatrix: Array[Array[Int]] = picks match
    case Some(pmap) if choices.nonEmpty && pmap.nonEmpty =>
      val n   = choices.size
      val mat = Array.ofDim[Int](n, n)
      pmap.values.foreach: ranking =>
        for (i <- choices.indices)
          for (j <- choices.indices)
            mat(i)(j) += (if (ranking(i) <= j) 1 else 0)
      mat
    case _ =>
      Array.ofDim[Int](0, 0)

object Ask:

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
      footer: Option[String]
  ) = Ask(
    _id = _id getOrElse (ornicar.scalalib.ThreadLocalRandom nextString 8),
    question = question,
    choices = choices,
    tags = tags,
    createdAt = java.time.Instant.now(),
    creator = creator,
    footer = footer,
    picks = None,
    feedback = None,
    url = None
  )

  def anonHash(eid: String, aid: Ask.ID): String =
    "anon-" + new String(base64.encode(com.roundeights.hasher.Algo.sha1(s"$eid-$aid").bytes))
      .substring(0, 11) // 66 bits is plenty of entropy, collisions are fairly harmless

  private val base64 = java.util.Base64.getEncoder().withoutPadding();
