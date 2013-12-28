package lila.app
package templating

import lila.chat.Env.{ current ⇒ chatEnv }
import lila.user.User

trait ChatHelper {

  def chatNameChans(chans: List[lila.chat.Chan], as: User): List[lila.chat.NamedChan] = chans match {
    case Nil ⇒ Nil
    case cs  ⇒ chatEnv.namer.chans(cs, as).await
  }
}
