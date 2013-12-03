package lila.report

import org.joda.time.DateTime
import ornicar.scalalib.Random

import lila.user.User

case class Report(
    id: String, // also the url slug
    user: String, // the reportee
    reason: String,
    text: String,
    processedBy: Option[String],
    createdAt: DateTime,
    createdBy: String) {

  def slug = id

  def isCreator(user: String) = user == createdBy

  def process(by: User) = copy(processedBy = by.id.some)

  def unprocessed = processedBy.isEmpty

  def realReason = Reason byName reason
}

object Report {

  def make(
    user: User,
    reason: Reason,
    text: String,
    createdBy: User): Report = new Report(
    id = Random nextString 8,
    user = user.id,
    reason = reason.name,
    text = text,
    processedBy = none,
    createdAt = DateTime.now,
    createdBy = createdBy.id)

  import lila.db.JsTube, JsTube.Helpers._
  import play.api.libs.json._

  private[report] lazy val tube = JsTube(
    (__.json update readDate('createdAt)) andThen Json.reads[Report],
    Json.writes[Report] andThen (__.json update writeDate('createdAt))
  )
}
