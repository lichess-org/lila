package lila.user

import lila.memo.BooleanExpiryMemo

import scala.concurrent.duration._
import scalaz.effects._

final class UsernameMemo(ttl: Duration) {

  private val internal = new BooleanExpiryMemo(ttl.toMillis.toInt)

  def normalize(name: String) = name.toLowerCase

  def get(key: String): Boolean = 
    internal get normalize(key)

  def put(key: String): IO[Unit] = 
    internal put normalize(key)

  def putAll(keys: Iterable[String]): IO[Unit] = 
    internal putAll (keys map normalize)

  def keys = internal.keys

  def preciseCount = internal.preciseCount
}
