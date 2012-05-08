package lila
package memo

import scalaz.effects._

final class UsernameMemo(timeout: Int) extends BooleanExpiryMemo(timeout) {

  def normalize(name: String) = name.toLowerCase

  override def get(key: String): Boolean = 
    super.get(normalize(key))

  override def put(key: String): IO[Unit] = 
    super.put(normalize(key))

  override def putAll(keys: Iterable[String]): IO[Unit] = 
    super.putAll(keys map normalize)
}
