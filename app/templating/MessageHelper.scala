package lila.app
package templating

import lila.message.Env.{ current => messageEnv }
import lila.report.Env.{ current => reportEnv }
import lila.api.Context

trait MessageHelper { self: SecurityHelper =>

  def messageNbUnread(ctx: Context): Int =
    ctx.me.??(user => messageEnv.api.unreadIds(user.id).await.size)

  def reportNbUnprocessed(implicit ctx: Context): Int =
    isGranted(_.SeeReport) ?? reportEnv.api.nbUnprocessed.await
}
