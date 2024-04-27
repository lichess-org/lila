package lila.core
package report

import lila.core.userId.UserId

case class SuspectId(value: UserId) extends AnyVal

case class CheatReportCreated(userId: UserId)

case class ScoreThresholds(mid: Int, high: Int)

trait ReportApi:
  def autoCommFlag(suspectId: SuspectId, resource: String, text: String, critical: Boolean = false): Funit
  def autoCheatReport(userId: UserId, text: String): Funit
  def autoCommReport(userId: UserId, text: String, critical: Boolean): Funit
  def maybeAutoPlaybanReport(userId: UserId, minutes: Int): Funit
