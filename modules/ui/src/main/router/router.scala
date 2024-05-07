// export for the play generated routers

package routes:

  export chess.Color
  export lila.core.id.*
  export lila.core.userId.UserStr
  export lila.core.perf.PerfKey
  export lila.ui.LilaRouter.given

package router:

  export lila.core.id.*
  export lila.core.userId.UserStr
  export lila.core.perf.PerfKey
  export lila.ui.LilaRouter.given

package router.router:

  export chess.Color
  export lila.core.id.*
  export lila.core.userId.UserStr
  export lila.core.perf.PerfKey
  export lila.ui.LilaRouter.given

package router.team:

  export lila.core.id.TeamId
  export lila.core.userId.UserStr
  export lila.ui.LilaRouter.given

package router.clas:

  export lila.core.id.{ ClasId, ClasInviteId }
  export lila.core.perf.PerfKey
  export lila.core.userId.UserStr
  export lila.ui.LilaRouter.given

package router.appeal:

  export lila.core.userId.UserStr
  export lila.ui.LilaRouter.given
