package lila.db

import reactivemongo.core.commands.LastError

package object api extends api.$operator {

  type JSFunction = String

  def successful(result: Fu[LastError]): Funit = 
    result flatMap { lastErr ⇒
      lastErr.ok.fold(funit, fufail(lastErr.stringify))
    }

}
