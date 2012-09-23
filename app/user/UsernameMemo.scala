package lila
package user

import memo.BooleanExpiryMemo

import scalaz.effects._

final class UsernameMemo(timeout: Int) {

  private val internal = new BooleanExpiryMemo(timeout)

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
