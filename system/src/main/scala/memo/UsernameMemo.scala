package lila.system
package memo

import scalaz.effects._

final class UsernameMemo(timeout: Int) extends BooleanExpiryMemo(timeout)
