package lila
package socket

final class Pinger {

  private val uids = new memo.PingMemo(20 * 1000)

  def apply(uid: String) {
    uids putUnsafe uid
  }
}
