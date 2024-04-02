package lila.core
package report

case class SuspectId(value: UserId) extends AnyVal
object SuspectId:
  def of(u: UserStr) = SuspectId(u.id)

case class CheatReportCreated(userId: UserId)

trait ReportApi:
  def autoCommFlag(suspectId: SuspectId, resource: String, text: String, critical: Boolean = false): Funit
  def autoCheatReport(userId: UserId, text: String): Funit
  def autoCommReport(userId: UserId, text: String, critical: Boolean): Funit
  def maybeAutoPlaybanReport(userId: UserId, minutes: Int): Funit
