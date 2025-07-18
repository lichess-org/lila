// export for the play generated routers

package routes:

  export scalalib.model.Days
  export chess.Color
  export chess.format.Uci
  export chess.opening.OpeningKey
  export lila.core.i18n.Language
  export lila.core.id.*
  export lila.core.userId.UserStr
  export lila.core.user.FlagCode
  export lila.core.perf.PerfKey
  export lila.core.socket.Sri
  export lila.core.study.Order as StudyOrder
  export lila.core.ublog.{ BlogsBy, QualityFilter as BlogQualityFilter }
  export lila.ui.LilaRouter.given

package router.router:

  export scalalib.model.Days
  export chess.Color
  export chess.format.Uci
  export chess.opening.OpeningKey
  export lila.core.i18n.Language
  export lila.core.id.*
  export lila.core.userId.UserStr
  export lila.core.user.FlagCode
  export lila.core.perf.PerfKey
  export lila.core.socket.Sri
  export lila.core.study.Order as StudyOrder
  export lila.core.ublog.{ BlogsBy, QualityFilter as BlogQualityFilter }
  export lila.ui.LilaRouter.given

package router.team:

  export lila.core.id.TeamId
  export lila.core.userId.UserStr
  export lila.ui.LilaRouter.given

package router.clas:

  export scalalib.model.Days
  export lila.core.id.{ ClasId, ClasInviteId }
  export lila.core.perf.PerfKey
  export lila.core.userId.UserStr
  export lila.ui.LilaRouter.given

package router.appeal:

  export lila.core.userId.UserStr
  export lila.ui.LilaRouter.given

package router.report:

  export lila.core.userId.UserStr
  export lila.core.id.ReportId
  export lila.ui.LilaRouter.given
