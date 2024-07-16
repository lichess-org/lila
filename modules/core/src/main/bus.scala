package lila.core
package bus

type Channel = String

// constructor is private so instances can only be created by extending the `GivenChannel` trait
final class WithChannel[T](private val key: Channel):
  def channel: Channel = key

transparent trait GivenChannel[T](val channel: Channel):
  given WithChannel[T] = WithChannel[T](channel)
