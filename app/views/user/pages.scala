package views.html.user

import lila.app.templating.Environment.{ *, given }
import lila.perfStat.{ PerfStat, PerfStatData }
import lila.rating.PerfType
import lila.core.data.SafeJsonStr

object download:

  private lazy val ui = lila.user.ui.UserGamesDownload(helpers)

  def apply(user: User)(using ctx: PageContext): Frag =
    views.html.base.layout(
      title = s"${user.username} • ${trans.site.exportGames.txt()}",
      moreCss = cssTag("search"),
      modules = jsModule("bits.userGamesDownload")
    )(ui(user))

object perfStat:

  lazy val ui = lila.perfStat.PerfStatUi(helpers)

  def apply(data: PerfStatData, ratingChart: Option[SafeJsonStr])(using PageContext) =
    import data.*
    import stat.perfType
    views.html.base.layout(
      title = s"${user.username} - ${trans.perfStat.perfStats.txt(perfType.trans)}",
      robots = false,
      modules = jsModule("bits.user") ++
        ratingChart.map { rc =>
          jsModuleInit(
            "chart.ratingHistory",
            SafeJsonStr(s"{data:$rc,singlePerfName:'${perfType.trans(using ctxTrans.translator.toDefault)}'}")
          ).some
        },
      moreCss = cssTag("perf-stat")
    ):
      ui.page(
        data,
        ratingChart.isDefined,
        side = show.page.side(user, ranks, perfType.key.some),
        perfTrophies = bits.perfTrophies(user, ranks.view.filterKeys(perfType == _).toMap)
      )
