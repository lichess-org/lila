package lidraughts.report

import lidraughts.user.User

case class Mod(user: User) extends AnyVal {
  def id = ModId(user.id)
}

case class ModId(value: User.ID) extends AnyVal
object ModId {
  def Lidraughts = ModId(lidraughts.user.User.lidraughtsId)
  def Irwin = ModId("irwin")
  def normalize(username: String) = ModId(User normalize username)
}

case class Suspect(user: User) extends AnyVal {
  def id = SuspectId(user.id)
  def set(f: User => User) = copy(user = f(user))
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
  def Lidraughts = ReporterId(lidraughts.user.User.lidraughtsId)
  def Irwin = ReporterId("irwin")
  implicit val reporterIdIso = lidraughts.common.Iso.string[ReporterId](ReporterId.apply, _.value)
}

case class Accuracy(value: Int) extends AnyVal
