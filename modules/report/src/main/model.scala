package lila.report

import lila.user.{ User, Me }

case class Mod(user: User) extends AnyVal:
  def id = user.id into ModId
object Mod:
  def me(me: Me): Mod = Mod(me.value)

case class Suspect(user: User) extends AnyVal:
  def id                   = SuspectId(user.id)
  def set(f: User => User) = Suspect(f(user))
case class SuspectId(value: UserId) extends AnyVal
object SuspectId:
  def of(u: UserStr) = SuspectId(u.id)

case class Victim(user: User.WithPerf) extends AnyVal

case class Reporter(user: User) extends AnyVal:
  def id = user.id into ReporterId

opaque type ReporterId = String
object ReporterId extends OpaqueUserId[ReporterId]:
  def lichess = User.lichessId into ReporterId
  def irwin   = User.irwinId into ReporterId
  def kaladin = User.kaladinId into ReporterId

opaque type Accuracy = Int
object Accuracy extends OpaqueInt[Accuracy]
