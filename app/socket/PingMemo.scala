package lila
package socket

import memo.BooleanExpiryMemo

// keys = uid
final class PingMemo(timeout: Int) extends BooleanExpiryMemo(timeout)
