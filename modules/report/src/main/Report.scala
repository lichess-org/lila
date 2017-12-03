package lila.report

import org.joda.time.DateTime
import ornicar.scalalib.Random
import scalaz.NonEmptyList

import lila.user.UserRepo.lichessId
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
) extends Reason.WithReason {

  import Report.{ Atom, Score }

  def id = _id
  def slug = _id

  def add(atom: Atom) = atomBy(atom.by).fold(copy(atoms = atom <:: atoms)) { existing =>
    val newAtom = existing.copy(
      at = atom.at,
      score = atom.score,
      text = s"${existing.text}\n\n${atom.text}"
    )
    copy(
      atoms = {
        newAtom :: atoms.toList.filterNot(_.by == atom.by)
      }.toNel | atoms
    )
  }.recomputeScore

  def recomputeScore = copy(
    score = atoms.toList.foldLeft(Score(0))(_ + _.score)
  )

  def recentAtom: Atom = atoms.head
  def oldestAtom: Atom = atoms.last
  def bestAtom: Atom = atoms.toList.sortBy(-_.score.value).headOption | recentAtom
  def onlyAtom: Option[Atom] = atoms.tail.isEmpty option atoms.head
  def nbOtherAtoms: Option[Int] = atoms.tail.nonEmpty option (atoms.size - 1)
  def atomBy(reporterId: ReporterId): Option[Atom] = atoms.toList.find(_.by == reporterId)

  def unprocessedCheat = unprocessed && isCheat
  def unprocessedOther = unprocessed && isOther
  def unprocessedTroll = unprocessed && isTroll
  def unprocessedInsult = unprocessed && isInsult
  def unprocessedTrollOrInsult = unprocessed && isTrollOrInsult

  def process(by: User) = copy(processedBy = by.id.some)

  def unprocessed = processedBy.isEmpty
  def processed = processedBy.isDefined

  def userIds: List[User.ID] = user :: atoms.toList.map(_.by.value)

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
  ) {
    def simplifiedText = text.lines.filterNot(_ startsWith "[AUTOREPORT]") mkString "\n"
  }

  case class Inquiry(mod: User.ID, seenAt: DateTime)

  case class WithSuspect(report: Report, suspect: Suspect, isOnline: Boolean) {

    def urgency: Int =
      (nowSeconds - report.recentAtom.at.getSeconds).toInt +
        (isOnline ?? (86400 * 5)) +
        (report.processed ?? Int.MinValue)
  }

  case class WithSuspectAndNotes(withSuspect: WithSuspect, notes: List[Note]) {
    def report = withSuspect.report
    def suspect = withSuspect.suspect
    def hasLichessNote = notes.exists(_.from == lichessId)
    def hasIrwinNote = notes.exists(_.from == "irwin")

    def userIds = report.userIds ::: notes.flatMap(_.userIds)
  }

  case class ByAndAbout(by: List[Report], about: List[Report]) {
    def userIds = by.flatMap(_.userIds) ::: about.flatMap(_.userIds)
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
    def isAutomatic = reporter.user.id == lichessId
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
