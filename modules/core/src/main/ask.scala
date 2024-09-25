package lila.core
package ask

import alleycats.Zero

import scalalib.extensions.{ *, given }
import lila.core.id.{ AskId }
import lila.core.userId.*

trait AskApi:
  def freeze(text: String, creator: UserId): Frozen
  def commit(frozen: Frozen, url: Option[String] = none[String]): Fu[Iterable[Ask]]
  def freezeAndCommit(text: String, creator: UserId, url: Option[String] = none[String]): Fu[String]
  def unfreezeAndLoad(text: String): Fu[String]
  def unfreeze(text: String): String
  def isOpen(aid: AskId): Fu[Boolean]
  def bake(text: String, askFrags: Iterable[String]): String
  val repo: AskRepo

trait AskRepo:
  def get(aid: AskId): Option[Ask]
  def getAsync(aid: AskId): Fu[Option[Ask]]
  def preload(text: String*): Fu[Boolean]
  def setPicks(aid: AskId, vid: String, picks: Option[Vector[Int]]): Fu[Option[Ask]]
  def setForm(aid: AskId, vid: String, form: Option[String]): Fu[Option[Ask]]
  def unset(aid: AskId, vid: String): Fu[Option[Ask]]
  def delete(aid: AskId): Funit
  def conclude(aid: AskId): Fu[Option[Ask]]
  def reset(aid: AskId): Fu[Option[Ask]]
  def deleteAll(text: String): Funit
  def asksIn(text: String): Fu[List[Option[Ask]]]
  def isOpen(aid: AskId): Fu[Boolean]
  def setUrl(text: String, url: Option[String]): Funit

case class Frozen(text: String, asks: Iterable[Ask])

case class Ask(
    _id: AskId,
    question: String,
    choices: Ask.Choices,
    tags: Ask.Tags,
    creator: UserId,
    createdAt: java.time.Instant,
    footer: Option[String], // optional text prompt for forms
    picks: Option[Ask.Picks],
    form: Option[Ask.Form],
    url: Option[String]
):

  // changes to any of the fields checked in compatible will invalidate votes and form
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
    if this.compatible(dbAsk) then // keep votes & form
      if tags.equals(dbAsk.tags) then dbAsk
      else dbAsk.copy(tags = tags)
    else copy(url = dbAsk.url) // discard votes & form

  def participants: Seq[String] = picks match
    case Some(p) => p.keys.filter(!_.startsWith("anon-")).toSeq
    case None    => Nil

  lazy val isOpen      = tags contains "open"               // allow votes from anyone (no acct reqired)
  lazy val isTraceable = tags.exists(_.startsWith("trace")) // everyone can see who voted for what
  lazy val isAnon = !isTraceable && tags.exists(_.startsWith("anon")) // hide voters from creator/mods
  lazy val isTally     = isTraceable || tags.contains("tally") // partial results viewable before conclusion
  lazy val isConcluded = tags contains "concluded"             // closed poll
  lazy val isRandom    = tags.exists(_.startsWith("random"))   // randomize order of choices
  lazy val isMulti    = !isRanked && tags.exists(_.startsWith("multi")) // multiple choices allowed
  lazy val isRanked   = tags.exists(_.startsWith("rank"))               // drag to sort
  lazy val isForm     = tags.exists(_.startsWith("form"))               // has a form/submit form
  lazy val isStretch  = tags.exists(_.startsWith("stretch"))            // stretch to fill width
  lazy val isVertical = tags.exists(_.startsWith("vert"))               // one choice per row
  lazy val isCheckbox = !isRanked && isVertical                         // use checkboxes
  lazy val isSubmit   = isForm || isRanked || tags.contains("submit")   // has a submit button

  def toAnon(user: UserId): Option[String] =
    Some(if isAnon then Ask.anonHash(user.value, _id) else user.value)

  def toAnon(ip: lila.core.net.IpAddress): Option[String] =
    isOpen.option(Ask.anonHash(ip.toString, _id))

  // eid = effective id, either a user id or an anonymous hash
  def hasPickFor(o: Option[String]): Boolean =
    o.fold(false)(eid => picks.exists(_.contains(eid)))

  def picksFor(o: Option[String]): Option[Vector[Int]] =
    o.flatMap(eid => picks.flatMap(_.get(eid)))

  def firstPickFor(o: Option[String]): Option[Int] =
    picksFor(o).flatMap(_.headOption)

  def hasFormFor(o: Option[String]): Boolean =
    o.fold(false)(eid => form.exists(_.contains(eid)))

  def formFor(o: Option[String]): Option[String] =
    o.flatMap(eid => form.flatMap(_.get(eid)))

  def count(choice: Int): Int    = picks.fold(0)(_.values.count(_ contains choice))
  def count(choice: String): Int = count(choices.indexOf(choice))

  def whoPicked(choice: String): List[String] = whoPicked(choices.indexOf(choice))
  def whoPicked(choice: Int): List[String] = picks
    .getOrElse(Nil)
    .collect:
      case (uid, ls) if ls contains choice => uid
    .toList

  def whoPickedAt(choice: Int, rank: Int): List[String] = picks
    .getOrElse(Nil)
    .collect:
      case (uid, ls) if ls.indexOf(choice) == rank => uid
    .toList

  @inline private def constrain(index: Int) = index.atMost(choices.size - 1).atLeast(0)

  def totals: Vector[Int] = picks match
    case Some(pmap) if choices.nonEmpty && pmap.nonEmpty =>
      val results = Array.ofDim[Int](choices.size)
      pmap.values.foreach(_.foreach { it => results(constrain(it)) += 1 })
      results.toVector
    case _ =>
      Vector.fill(choices.size)(0)

  // index of returned vector maps to choices list, values from [0f, choices.size-1f] where 0 is "best" rank
  def averageRank: Vector[Float] = picks match
    case Some(pmap) if choices.nonEmpty && pmap.nonEmpty =>
      val results = Array.ofDim[Int](choices.size)
      pmap.values.foreach: ranking =>
        for it <- choices.indices do results(constrain(ranking(it))) += it
      results.map(_ / pmap.size.toFloat).toVector
    case _ =>
      Vector.fill(choices.size)(0f)

  // an [n]x[n-1] matrix M describing response rankings. each element M[i][j] is the number of
  // respondents who preferred the choice i at rank j or below (effectively in the top j+1 picks)
  // the rightmost column is omitted as it is always equal to the number of respondents
  def rankMatrix: Array[Array[Int]] = picks match
    case Some(pmap) if choices.nonEmpty && pmap.nonEmpty =>
      val n   = choices.size - 1
      val mat = Array.ofDim[Int](choices.size, n)
      pmap.values.foreach: ranking =>
        for i <- choices.indices do
          val iRank = ranking.indexOf(i)
          for j <- iRank until n do mat(i)(j) += (if iRank <= j then 1 else 0)
      mat
    case _ =>
      Array.ofDim[Int](0, 0)

  def toJson: play.api.libs.json.JsObject =
    play.api.libs.json.Json.obj(
      "id"       -> _id.value,
      "question" -> question,
      "choices"  -> choices,
      "tags"     -> tags,
      "creator"  -> creator.value,
      "created"  -> createdAt.toString,
      "footer"   -> footer,
      "picks"    -> picks,
      "form"     -> form,
      "url"      -> url
    )

object Ask:

  // type ID      = AskId
  type Tags    = Set[String]
  type Choices = Vector[String]
  type Picks   = Map[String, Vector[Int]] // ranked list of indices into Choices vector
  type Form    = Map[String, String]

  // https://www.unicode.org/faq/private_use.html
  val frozenIdMagic = "\ufdd6\ufdd4\ufdd2\ufdd0"
  val frozenIdRe    = s"$frozenIdMagic\\{(\\S{8})}".r

  def make(
      _id: Option[String],
      question: String,
      choices: Choices,
      tags: Tags,
      creator: UserId,
      footer: Option[String],
      url: Option[String]
  ) = Ask(
    _id = AskId(_id.getOrElse(scalalib.ThreadLocalRandom.nextString(8))),
    question = question,
    choices = choices,
    tags = tags,
    createdAt = java.time.Instant.now(),
    creator = creator,
    footer = footer,
    picks = None,
    form = None,
    url = None
  )

  def strip(text: String, n: Int = -1): String =
    frozenIdRe.replaceAllIn(text, "").take(if n == -1 then text.length else n)

  def anonHash(text: String, aid: AskId): String =
    "anon-" + base64
      .encodeToString(
        java.security.MessageDigest.getInstance("SHA-1").digest(s"$text-$aid".getBytes("UTF-8"))
      )
      .substring(0, 11)

  private lazy val base64 = java.util.Base64.getEncoder().withoutPadding();
