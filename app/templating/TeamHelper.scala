package lila.app
package templating

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._

trait TeamHelper { self: HasEnv =>

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ctx.me.??(me => env.team.api.syncBelongsTo(teamId, me.id))

  def teamIdToName(id: String): Frag = StringFrag(env.team.getTeamName(id).getOrElse(id))

}
