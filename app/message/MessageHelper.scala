package lila
package message

import core.CoreEnv
import http.Context

trait MessageHelper { 

  protected def env: CoreEnv

  def messageNbUnread(ctx: Context) = ~ctx.me.map(env.message.unreadCache.get)
}
