package lila.app
package templating

import lila.message.Env.{ current ⇒ messageEnv }
import lila.user.Context

trait MessageHelper {

  def messageNbUnread(ctx: Context): Int =
    ctx.me.??(user ⇒ messageEnv.api.unreadIds(user.id).await.size)
}
