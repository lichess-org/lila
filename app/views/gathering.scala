package views.html

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.gathering.Condition
import lila.rating.PerfType

object gathering:

  def verdicts(vs: Condition.WithVerdicts, pt: PerfType, relevant: Boolean = true)(using
      ctx: Context
  ): Option[Tag] =
    vs.nonEmpty option st.section(
      dataIcon := relevant.option(if ctx.isAuth && vs.accepted then licon.Checkmark else licon.Padlock),
      cls := List(
        "conditions" -> true,
        "accepted"   -> (relevant && ctx.isAuth && vs.accepted),
        "refused"    -> (relevant && ctx.isAuth && !vs.accepted)
      )
    )(
      div(
        vs.list.sizeIs < 2 option p(trans.conditionOfEntry()),
        vs.list.map: v =>
          p(
            cls := List(
              "condition" -> true,
              "accepted"  -> (relevant && ctx.isAuth && v.verdict.accepted),
              "refused"   -> (relevant && ctx.isAuth && !v.verdict.accepted)
            ),
            title := relevant option v.verdict.reason.map(_(ctx.lang))
          ):
            v.condition match
              case Condition.TeamMember(teamId, teamName) =>
                trans.mustBeInTeam(teamLink(teamId, teamName, withIcon = false))
              case condition =>
                v.verdict match
                  case Condition.RefusedUntil(until) =>
                    frag(
                      "Because you missed your last Swiss game, you cannot enter a new Swiss tournament until ",
                      absClientInstant(until),
                      "."
                    )
                  case _ => condition.name(pt)
      )
    )
