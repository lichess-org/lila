package lila.report

import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.ThreadLocalRandom

import lila.core.id.ReportId
import lila.core.perf.UserWithPerfs
import lila.core.report.SuspectId
import lila.core.userId.ModId

case class Report(
    @Key("_id") id: ReportId, // also the url slug
    user: UserId,             // the reportee
    reason: Reason,
    room: Room,
    atoms: NonEmptyList[Report.Atom], // most recent first
    score: Report.Score,
    inquiry: Option[Report.Inquiry],
    open: Boolean,
    done: Option[Report.Done]
) extends Reason.WithReason:

  import Report.{ Atom, Score }

  // private given Ordering[Double] = scala.math.Ordering.Double.TotalOrdering

  inline def slug = id

  def closed  = !open
  def suspect = SuspectId(user)

  def add(atom: Atom) =
    atomBy(atom.by)
      .fold(copy(atoms = atom :: atoms)): existing =>
        if existing.text.contains(atom.text) then this
        else
          copy(
            atoms = {
              existing.copy(
                at = atom.at,
                score = atom.score.atLeast(existing.score),
                text = s"${existing.text}\n\n${atom.text}"
              ) :: atoms.toList.filterNot(_.by == atom.by)
            }.toNel | atoms
          )
      .recomputeScore

  def recomputeScore =
    copy(score = atoms.toList.foldLeft(Score(0))(_ + _.score))

  def recentAtom: Atom = atoms.head
  def oldestAtom: Atom = atoms.last
  def bestAtom: Atom   = bestAtoms(1).headOption | recentAtom
  def bestAtoms(nb: Int): List[Atom] =
    atoms.toList
      .sortBy: a =>
        (-a.score.value, -a.at.toSeconds)
      .take(nb)
  def onlyAtom: Option[Atom]                       = atoms.tail.isEmpty.option(atoms.head)
  def atomBy(reporterId: ReporterId): Option[Atom] = atoms.toList.find(_.by == reporterId)
  def bestAtomByHuman: Option[Atom]                = bestAtoms(10).find(_.byHuman)

  def unprocessedCheat = open && isCheat
  def unprocessedOther = open && isOther
  def unprocessedComm  = open && isComm

  def process(by: User) =
    copy(
      open = false,
      done = Report.Done(by.id.into(ModId), nowInstant).some
    )

  def userIds: List[UserId] = user :: atoms.toList.map(_.by.userId)

  def isRecentComm                 = open && room == Room.Comm
  def isRecentCommOf(sus: Suspect) = isRecentComm && user == sus.user.id

  def isAppeal = room == Room.Other && atoms.head.text == Report.appealText

  def isSpontaneous = room == Room.Other && atoms.head.text == Report.spontaneousText

  def isAlreadySlain(sus: User) =
    (isCheat && sus.marks.engine) || (isBoost && sus.marks.boost) || (isComm && sus.marks.troll)

object Report:

  opaque type Score = Double
  object Score extends OpaqueDouble[Score]:
    extension (a: Score)
      def +(s: Score): Score = a + s
      def color =
        if a >= 150 then "red"
        else if a >= 100 then "orange"
        else if a >= 50 then "yellow"
        else "green"
      def atLeast(s: Score): Score = math.max(a, s)
      def withinBounds: Score      = a.atLeast(5).atMost(100)

  case class Atom(
      by: ReporterId,
      text: String,
      score: Score,
      at: Instant
  ):
    def simplifiedText = text.linesIterator.filterNot(_.startsWith("[AUTOREPORT]")).mkString("\n")

    def byHuman = !byLichess && by.isnt(ReporterId.irwin)

    def byLichess = by.is(ReporterId.lichess)

    def isFlag = text.startsWith(Reason.flagText)

  case class Done(by: ModId, at: Instant)

  case class Inquiry(mod: UserId, seenAt: Instant)

  case class WithSuspect(report: Report, suspect: UserWithPerfs, isOnline: Boolean):
    def urgency: Int =
      report.score.value.toInt +
        (isOnline.so(1000)) +
        (report.closed.so(-999999))

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

  object Candidate:
    case class Scored(candidate: Candidate, score: Score):
      def withScore(f: Score => Score) = copy(score = f(score))
      def atom =
        Atom(
          by = candidate.reporter.id,
          text = candidate.text,
          score = score,
          at = nowInstant
        )

  private[report] val spontaneousText = "Spontaneous inquiry"
  private[report] val appealText      = "Appeal"

  def make(c: Candidate.Scored, existing: Option[Report]) =
    import c.*
    existing.fold(
      Report(
        id = ReportId(ThreadLocalRandom.nextString(8)),
        user = candidate.suspect.user.id,
        reason = candidate.reason,
        room = Room(candidate.reason),
        atoms = NonEmptyList.one(c.atom),
        score = score,
        inquiry = none,
        open = true,
        done = none
      )
    )(_.add(c.atom))

  private[report] case class SnoozeKey(snoozerId: UserId, reportId: ReportId)
