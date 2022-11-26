package lila.report

import org.joda.time.DateTime
import cats.data.NonEmptyList

import lila.user.User
import lila.common.Iso

case class Report(
    _id: Report.ID, // also the url slug
    user: User.ID,  // the reportee
    reason: Reason,
    room: Room,
    atoms: NonEmptyList[Report.Atom], // most recent first
    score: Report.Score,
    inquiry: Option[Report.Inquiry],
    open: Boolean,
    done: Option[Report.Done]
) extends Reason.WithReason:

  import Report.{ Atom, Score }

  private given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

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
                score = atom.score atLeast existing.score,
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
      done = Report.Done(by.id, DateTime.now).some
    )

  def userIds: List[User.ID] = user :: atoms.toList.map(_.by.value)

  def isRecentComm                 = open && room == Room.Comm
  def isRecentCommOf(sus: Suspect) = isRecentComm && user == sus.user.id

  def isAppeal = room == Room.Other && atoms.head.text == Report.appealText

  def isSpontaneous = room == Room.Other && atoms.head.text == Report.spontaneousText

  def isAlreadySlain(sus: User) =
    (isCheat && sus.marks.engine) || (isBoost && sus.marks.boost) || (isComm && sus.marks.troll)

object Report:

  type ID = String

  case class Score(value: Double) extends AnyVal:
    def +(s: Score) = Score(s.value + value)
    def *(m: Int)   = Score(value * m)
    def /(m: Int)   = Score(value / m)
    def color =
      if (value >= 150) "red"
      else if (value >= 100) "orange"
      else if (value >= 50) "yellow"
      else "green"
    def atLeast(v: Int)   = Score(value atLeast v)
    def atLeast(s: Score) = Score(value atLeast s.value)
    def withinBounds      = Score(value atLeast 5 atMost 100)
  given Iso.DoubleIso[Score] = Iso.double[Score](Score.apply, _.value)

  case class Atom(
      by: ReporterId,
      text: String,
      score: Score,
      at: DateTime
  ):
    def simplifiedText = text.linesIterator.filterNot(_ startsWith "[AUTOREPORT]") mkString "\n"

    def byHuman = !byLichess && by != ReporterId.irwin

    def byLichess = by == ReporterId.lichess

  case class Done(by: User.ID, at: DateTime)

  case class Inquiry(mod: User.ID, seenAt: DateTime)

  case class WithSuspect(report: Report, suspect: Suspect, isOnline: Boolean):

    def urgency: Int =
      report.score.value.toInt +
        (isOnline ?? 1000) +
        (report.closed ?? -999999)

  case class ByAndAbout(by: List[Report], about: List[Report]):
    def userIds = by.flatMap(_.userIds) ::: about.flatMap(_.userIds)

  case class Candidate(
      reporter: Reporter,
      suspect: Suspect,
      reason: Reason,
      text: String
  ) extends Reason.WithReason:
    def scored(score: Score) = Candidate.Scored(this, score)
    def isAutomatic          = reporter.id == ReporterId.lichess
    def isAutoComm           = isAutomatic && isComm
    def isAutoBoost          = isAutomatic && isBoost
    def isIrwinCheat         = reporter.id == ReporterId.irwin && isCheat
    def isKaladinCheat       = reporter.id == ReporterId.kaladin && isCheat
    def isCoachReview        = isOther && text.contains("COACH REVIEW")

  object Candidate:
    case class Scored(candidate: Candidate, score: Score):
      def withScore(f: Score => Score) = copy(score = f(score))
      def atom =
        Atom(
          by = candidate.reporter.id,
          text = candidate.text,
          score = score,
          at = DateTime.now
        )

  private[report] val spontaneousText = "Spontaneous inquiry"
  private[report] val appealText      = "Appeal"

  def make(c: Candidate.Scored, existing: Option[Report]) =
    c match
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
            done = none
          )
        )(_ add c.atom)

  private[report] case class SnoozeKey(snoozerId: User.ID, reportId: Report.ID) extends lila.memo.Snooze.Key
