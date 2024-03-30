package lila.hub
package report

case class SuspectId(value: UserId) extends AnyVal
object SuspectId:
  def of(u: UserStr) = SuspectId(u.id)
