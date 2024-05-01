package lila.core
package bus

trait Tellable extends Any:
  def !(msg: Matchable): Unit

type Channel    = String
type Subscriber = Tellable
type Payload    = Matchable

final class WithChannel[T](val channel: Channel)
