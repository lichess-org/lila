package lila.app
package templating

import lila.relation.Relation
import lila.user.Context

trait RelationHelper {

  private def api = Env.relation.api

  def relationWith(userId: String)(implicit ctx: Context): Option[Relation] =
    ctx.userId flatMap { api.relation(_, userId).await }
}
