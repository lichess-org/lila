package lila.system
package memo

final class WatcherMemo(timeout: Int) extends BooleanExpiryMemo(timeout) {

  def count(prefix: String): Int = keys count (_ startsWith prefix)
}
