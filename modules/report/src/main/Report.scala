package lila.report

import org.joda.time.DateTime
import ornicar.scalalib.Random

import lila.user.{ User, Note }

case class Report(
    _id: Report.ID, // also the url slug
    user: User.ID, // the reportee
    reason: Reason,
    room: Room,
    text: String,
    inquiry: Option[Report.Inquiry],
    processedBy: Option[User.ID],
    createdAt: DateTime,
    createdBy: User.ID
) {

  def id = _id
  def slug = _id

  def isCreator(user: User.ID) = user == createdBy

  def isCheat = reason == Reason.Cheat
  def isOther = reason == Reason.Other
  def isTroll = reason == Reason.Troll
  def isInsult = reason == Reason.Insult
  def isPrint = reason == Reason.CheatPrint
  def isTrollOrInsult = reason == Reason.Troll || reason == Reason.Insult

  def unprocessedCheat = unprocessed && isCheat
  def unprocessedOther = unprocessed && isOther
  def unprocessedTroll = unprocessed && isTroll
  def unprocessedInsult = unprocessed && isInsult
  def unprocessedTrollOrInsult = unprocessed && isTrollOrInsult

  def isAutomatic = createdBy == "lichess"
  def isManual = !isAutomatic

  def process(by: User) = copy(processedBy = by.id.some)

  def unprocessed = processedBy.isEmpty
  def processed = processedBy.isDefined

  def userIds = List(user, createdBy)

  def simplifiedText = text.lines.filterNot(_ startsWith "[AUTOREPORT]") mkString "\n"

  def isRecentComm = room == Room.Coms && !processed
  def isRecentCommOf(sus: Suspect) = isRecentComm && user == sus.user.id
}

object Report {

  type ID = String

  case class Inquiry(mod: User.ID, seenAt: DateTime)

  case class WithUser(report: Report, user: User, isOnline: Boolean, accuracy: Option[Int]) {

    def urgency: Int =
      (nowSeconds - report.createdAt.getSeconds).toInt +
        (isOnline ?? (86400 * 5)) +
        (report.processed ?? Int.MinValue)
  }

  case class WithUserAndNotes(withUser: WithUser, notes: List[Note]) {
    def report = withUser.report
    def user = withUser.user
    def hasLichessNote = notes.exists(_.from == "lichess")
    def hasIrwinNote = notes.exists(_.from == "irwin")

    def userIds = report.userIds ::: notes.flatMap(_.userIds)
  }

  case class ByAndAbout(by: List[Report], about: List[Report]) {
    def userIds = by.flatMap(_.userIds) ::: about.flatMap(_.userIds)
  }

  private[report] val spontaneousText = "Spontaneous inquiry"

  def make(
    suspect: Suspect,
    reason: Reason,
    text: String,
    reporter: Reporter
  ): Report = new Report(
    _id = Random nextString 8,
    user = suspect.user.id,
    reason = reason,
    room = Room(reason),
    text = text,
    inquiry = none,
    processedBy = none,
    createdAt = DateTime.now,
    createdBy = reporter.user.id
  )
}
