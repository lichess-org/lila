package lila.report

import lila.user.User
import lila.common.Iso

case class Mod(user: User) extends AnyVal:
  def id = ModId(user.id)

case class ModId(value: UserId) extends AnyVal
object ModId:
  def lichess                     = ModId(lila.user.User.lichessId)
  def irwin                       = ModId("irwin")
  def kaladin                     = ModId("kaladin")
  def normalize(username: String) = ModId(User normalize username)

case class Suspect(user: User) extends AnyVal:
  def id                   = SuspectId(user.id)
  def set(f: User => User) = Suspect(f(user))
case class SuspectId(value: UserId) extends AnyVal
object SuspectId:
  def normalize(username: String) = SuspectId(User normalize username)

case class Victim(user: User) extends AnyVal

case class Reporter(user: User) extends AnyVal:
  def id = ReporterId(user.id)
case class ReporterId(value: UserId) extends AnyVal

object ReporterId:
  def lichess                     = ReporterId(lila.user.User.lichessId)
  def irwin                       = ReporterId("irwin")
  def kaladin                     = ReporterId("kaladin")
  given Iso.StringIso[ReporterId] = Iso.string[ReporterId](ReporterId.apply, _.value)

case class Accuracy(value: Int) extends AnyVal
