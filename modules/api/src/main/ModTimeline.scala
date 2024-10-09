package lila.api

case class ModTimeline(
    modLog: List[Modlog],
    appeal: Option[Appeal]
)

val modLog = for
  history <- env.mod.logApi.userHistory(user.id)
  appeal  <- isGranted(_.Appeals).so(env.appeal.api.byId(user))
yield views.user.mod.modLog(history, appeal)
