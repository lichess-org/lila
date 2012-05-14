package lila
package lobby

import memo.BooleanExpiryMemo

// keys = ownerId
final class HookMemo(timeout: Int) extends BooleanExpiryMemo(timeout)
