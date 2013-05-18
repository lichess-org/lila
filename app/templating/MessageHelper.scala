package lila.app
package templating

import lila.message.Env.{ current ⇒ messageEnv }
import lila.user.Context

trait MessageHelper {

  def messageNbUnread(ctx: Context) =
    ctx.me.??(user ⇒ messageEnv.api.nbUnreadMessages(user.id).await)
}
