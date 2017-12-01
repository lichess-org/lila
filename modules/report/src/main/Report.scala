package lila.report

import org.joda.time.DateTime
import ornicar.scalalib.Random
import scalaz.NonEmptyList

import lila.user.{ User, Note }

case class Report(
    _id: Report.ID, // also the url slug
    user: User.ID, // the reportee
    reason: Reason,
    room: Room,
    atoms: NonEmptyList[Report.Atom], // most recent first
    score: Report.Score,
    inquiry: Option[Report.Inquiry],
    processedBy: Option[User.ID]
) extends WithReason {

  import Report.{ Atom, Score }

  def id = _id
  def slug = _id

  def add(atom: Atom) = copy( // atoms = atom +: atoms
  ).recomputeScore

  def recomputeScore = copy(
    score = atoms.toList.foldLeft(Score(0))(_ + _.score)
  )

  def lastAtom = atoms.last

  // def isCreator(user: User.ID) = user == createdBy

  def unprocessedCheat = unprocessed && isCheat
  def unprocessedOther = unprocessed && isOther
  def unprocessedTroll = unprocessed && isTroll
  def unprocessedInsult = unprocessed && isInsult
  def unprocessedTrollOrInsult = unprocessed && isTrollOrInsult

  // def isAutomatic = createdBy == "lichess"
  // def isManual = !isAutomatic

  def process(by: User) = copy(processedBy = by.id.some)

  def unprocessed = processedBy.isEmpty
  def processed = processedBy.isDefined

  // def userIds = List(user, createdBy)

  def bestAtom: Atom = atoms.toList.sortBy(-_.score.value).headOption | atoms.head

  def simplifiedText = bestAtom.text.lines.filterNot(_ startsWith "[AUTOREPORT]") mkString "\n"

  def isRecentComm = room == Room.Coms && !processed
  def isRecentCommOf(sus: Suspect) = isRecentComm && user == sus.user.id
}

object Report {

  type ID = String

  case class Score(value: Double) extends AnyVal {
    def +(s: Score) = Score(s.value + value)
  }
  implicit val scoreIso = lila.common.Iso.double[Score](Score.apply, _.value)

  case class Atom(
      by: ReporterId,
      text: String,
      score: Score,
      at: DateTime
  )

  case class Inquiry(mod: User.ID, seenAt: DateTime)

  case class WithUser(report: Report, user: User, isOnline: Boolean, accuracy: Option[Int]) {

    // def urgency: Int =
    //   (nowSeconds - report.createdAt.getSeconds).toInt +
    //     (isOnline ?? (86400 * 5)) +
    //     (report.processed ?? Int.MinValue)
  }

  case class WithUserAndNotes(withUser: WithUser, notes: List[Note]) {
    def report = withUser.report
    def user = withUser.user
    def hasLichessNote = notes.exists(_.from == "lichess")
    def hasIrwinNote = notes.exists(_.from == "irwin")

    // def userIds = report.userIds ::: notes.flatMap(_.userIds)
  }

  case class ByAndAbout(by: List[Report], about: List[Report]) {
    // def userIds = by.flatMap(_.userIds) ::: about.flatMap(_.userIds)
  }

  case class Candidate(
      reporter: Reporter,
      suspect: Suspect,
      reason: Reason,
      text: String
  ) extends Reason.WithReason {
    def atom = Atom(
      by = reporter.id,
      text = text,
      score = getScore(this),
      at = DateTime.now
    )
  }

  private[report] val spontaneousText = "Spontaneous inquiry"

  def getScore(candidate: Candidate) = Score(1)

  def make(candidate: Candidate, existing: Option[Report]) =
    existing.map(_ add candidate.atom) | Report(
      _id = Random nextString 8,
      user = candidate.suspect.user.id,
      reason = candidate.reason,
      room = Room(candidate.reason),
      atoms = NonEmptyList(candidate.atom),
      score = candidate.atom.score,
      inquiry = none,
      processedBy = none
    )
}
