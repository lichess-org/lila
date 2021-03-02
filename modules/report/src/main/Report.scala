package lila.report

import org.joda.time.DateTime
import cats.data.NonEmptyList

import lila.user.User

case class Report(
    _id: Report.ID, // also the url slug
    user: User.ID,  // the reportee
    reason: Reason,
    room: Room,
    atoms: NonEmptyList[Report.Atom], // most recent first
    score: Report.Score,
    inquiry: Option[Report.Inquiry],
    open: Boolean,
    processedBy: Option[User.ID]
) extends Reason.WithReason {

  import Report.{ Atom, Score }

  implicit private val ordering = scala.math.Ordering.Double.TotalOrdering

  def id   = _id
  def slug = _id

  def closed  = !open
  def suspect = SuspectId(user)

  def add(atom: Atom) =
    atomBy(atom.by)
      .fold(copy(atoms = atom :: atoms)) { existing =>
        if (existing.text contains atom.text) this
        else
          copy(
            atoms = {
              existing.copy(
                at = atom.at,
                score = atom.score,
                text = s"${existing.text}\n\n${atom.text}"
              ) :: atoms.toList.filterNot(_.by == atom.by)
            }.toNel | atoms
          )
      }
      .recomputeScore

  def recomputeScore =
    copy(
      score = atoms.toList.foldLeft(Score(0))(_ + _.score)
    )

  def recentAtom: Atom = atoms.head
  def oldestAtom: Atom = atoms.last
  def bestAtom: Atom   = bestAtoms(1).headOption | recentAtom
  def bestAtoms(nb: Int): List[Atom] =
    atoms.toList.sortBy { a =>
      (-a.score.value, -a.at.getSeconds)
    } take nb
  def onlyAtom: Option[Atom]                       = atoms.tail.isEmpty option atoms.head
  def atomBy(reporterId: ReporterId): Option[Atom] = atoms.toList.find(_.by == reporterId)
  def bestAtomByHuman: Option[Atom]                = bestAtoms(10).find(_.byHuman)

  def unprocessedCheat = open && isCheat
  def unprocessedOther = open && isOther
  def unprocessedComm  = open && isComm

  def process(by: User) =
    copy(
      open = false,
      processedBy = by.id.some
    )

  def userIds: List[User.ID] = user :: atoms.toList.map(_.by.value)

  def isRecentComm                 = room == Room.Comm && open
  def isRecentCommOf(sus: Suspect) = isRecentComm && user == sus.user.id

  def isAppeal = room == Room.Other && atoms.head.text == Report.appealText
}

object Report {

  type ID = String

  case class Score(value: Double) extends AnyVal {
    def +(s: Score) = Score(s.value + value)
    def color =
      if (value >= 150) "red"
      else if (value >= 100) "orange"
      else if (value >= 50) "yellow"
      else "green"
  }
  implicit val scoreIso = lila.common.Iso.double[Score](Score.apply, _.value)

  case class Atom(
      by: ReporterId,
      text: String,
      score: Score,
      at: DateTime
  ) {
    def simplifiedText = text.linesIterator.filterNot(_ startsWith "[AUTOREPORT]") mkString "\n"

    def byHuman = !byLichess && by != ReporterId.irwin

    def byLichess = by == ReporterId.lichess
  }

  case class Inquiry(mod: User.ID, seenAt: DateTime)

  case class WithSuspect(report: Report, suspect: Suspect, isOnline: Boolean) {

    def urgency: Int =
      report.score.value.toInt +
        (isOnline ?? 1000) +
        (report.closed ?? -999999)
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
    def scored(score: Score) = Candidate.Scored(this, score)
    def isAutomatic          = reporter.id == ReporterId.lichess
    def isAutoComm           = isAutomatic && isComm
    def isCoachReview        = isOther && text.contains("COACH REVIEW")
    def isCommFlag           = text contains Reason.Comm.flagText
  }

  object Candidate {
    case class Scored(candidate: Candidate, score: Score) {
      def withScore(f: Score => Score) = copy(score = f(score))
      def atom =
        Atom(
          by = candidate.reporter.id,
          text = candidate.text,
          score = score,
          at = DateTime.now
        )
    }
  }

  private[report] val spontaneousText = "Spontaneous inquiry"
  private[report] val appealText      = "Appeal"

  def make(c: Candidate.Scored, existing: Option[Report]) =
    c match {
      case c @ Candidate.Scored(candidate, score) =>
        existing.fold(
          Report(
            _id = lila.common.ThreadLocalRandom nextString 8,
            user = candidate.suspect.user.id,
            reason = candidate.reason,
            room = Room(candidate.reason),
            atoms = NonEmptyList.one(c.atom),
            score = score,
            inquiry = none,
            open = true,
            processedBy = none
          )
        )(_ add c.atom)
    }
}
