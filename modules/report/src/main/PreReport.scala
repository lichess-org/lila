package lila.report

import org.joda.time.DateTime
import ornicar.scalalib.Random

import lila.user.{ User, Note }

case class PreReport(
    _id: PreReport.ID, // also the url slug
    user: User.ID, // the reportee
    reason: Reason,
    text: String,
    createdAt: DateTime,
    createdBy: User.ID
) extends WithReason {

  def id = _id
  def slug = _id

  def isCreator(user: User.ID) = user == createdBy
}

object PreReport {

  type ID = String

  def make(
    suspect: Suspect,
    reason: Reason,
    text: String,
    reporter: Reporter
  ): PreReport = new PreReport(
    _id = Random nextString 8,
    user = suspect.user.id,
    reason = reason,
    text = text,
    createdAt = DateTime.now,
    createdBy = reporter.user.id
  )
}
