package lila.report

import lila.user.User

case class Mod(user: User) extends AnyVal {
  def id = ModId(user.id)
}

case class ModId(value: User.ID) extends AnyVal
object ModId {
  def lichess                     = ModId(lila.user.User.lichessId)
  def irwin                       = ModId("irwin")
  def normalize(username: String) = ModId(User normalize username)
}

case class Suspect(user: User) extends AnyVal {
  def id                   = SuspectId(user.id)
  def set(f: User => User) = Suspect(f(user))
}
case class SuspectId(value: User.ID) extends AnyVal
object SuspectId {
  def normalize(username: String) = SuspectId(User normalize username)
}

case class Victim(user: User) extends AnyVal

case class Reporter(user: User) extends AnyVal {
  def id = ReporterId(user.id)
}
case class ReporterId(value: User.ID) extends AnyVal

object ReporterId {
  def lichess                = ReporterId(lila.user.User.lichessId)
  def irwin                  = ReporterId("irwin")
  implicit val reporterIdIso = lila.common.Iso.string[ReporterId](ReporterId.apply, _.value)
}

case class Accuracy(value: Int) extends AnyVal

object Snooze {
  import scala.concurrent.duration._
  sealed abstract class Duration(val value: FiniteDuration, val name: String)
  object Duration {
    case object TwentyMinutes extends Duration(20 minutes, "20 minutes")
    case object OneHour       extends Duration(1 hour, "one hour")
    case object ThreeHours    extends Duration(3 hours, "three hours")
    case object OneDay        extends Duration(1 day, "one day")
    case object NextDeploy    extends Duration(30 days, "until next deploy")
    val all                = List(TwentyMinutes, OneHour, ThreeHours, OneDay, NextDeploy)
    def apply(key: String) = all.find(_.toString == key)
  }
  private[report] case class SnoozeKey(modId: ModId, reportId: Report.ID)
}
