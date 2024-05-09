package lila.core
package bus

type Channel = String

final class WithChannel[T](val channel: Channel)
