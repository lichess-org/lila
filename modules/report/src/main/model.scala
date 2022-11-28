package lila.report

import lila.user.User
import lila.common.Iso

case class Mod(user: User) extends AnyVal:
  def id = ModId(user.id)

case class ModId(value: UserId) extends AnyVal
object ModId:
  def lichess = ModId(User.lichessId)
  def irwin   = ModId(User.irwinId)
  def kaladin = ModId(User.kaladinId)

case class Suspect(user: User) extends AnyVal:
  def id                   = SuspectId(user.id)
  def set(f: User => User) = Suspect(f(user))
case class SuspectId(value: UserId) extends AnyVal
object SuspectId:
  def of(u: UserStr) = SuspectId(u.id)

case class Victim(user: User) extends AnyVal

case class Reporter(user: User) extends AnyVal:
  def id = user.id into ReporterId

opaque type ReporterId = String
object ReporterId extends OpaqueUserId[ReporterId]:
  def lichess = User.lichessId into ReporterId
  def irwin   = User.irwinId into ReporterId
  def kaladin = User.kaladinId into ReporterId

case class Accuracy(value: Int) extends AnyVal
