package lila.report

import lila.core.report.SuspectId
import lila.core.user.WithPerf
import lila.core.userId.{ ModId, OpaqueUserId }

case class Mod(user: User) extends AnyVal:
  def id = user.id.into(ModId)
object Mod:
  def me(me: Me): Mod = Mod(me.value)

case class Suspect(user: User) extends AnyVal:
  def id = SuspectId(user.id)
  def set(f: User => User) = Suspect(f(user))

case class Victim(user: WithPerf) extends AnyVal

case class Reporter(user: User) extends AnyVal:
  def id = user.id.into(ReporterId)

opaque type ReporterId = String
object ReporterId extends OpaqueUserId[ReporterId]:
  given UserIdOf[ReporterId] = UserId(_)
  def lichess = UserId.lichess.into(ReporterId)
  def irwin = UserId.irwin.into(ReporterId)
  def kaladin = UserId.kaladin.into(ReporterId)
  def ai = UserId.ai.into(ReporterId)

opaque type Accuracy = Int
object Accuracy extends OpaqueInt[Accuracy]
