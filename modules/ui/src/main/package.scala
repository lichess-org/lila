package lila.ui

import play.api.libs.json.JsValue
import scalatags.Text.all.Frag

import lila.core.i18n.I18nKey
import lila.core.perf.{ KeyedPerf, UserPerfs }
import lila.core.data.SafeJsonStr

export lila.core.lilaism.Lilaism.{ *, given }

trait RatingApi:
  val toNameKey: PerfKey => I18nKey
  val toDescKey: PerfKey => I18nKey
  val toIcon: PerfKey => Icon
  val bestRated: UserPerfs => Option[KeyedPerf]
  val dubiousPuzzle: UserPerfs => Boolean

case class PageModule(name: String, data: JsValue | SafeJsonStr)
case class EsmInit(key: String, init: Frag)
type EsmList = List[Option[EsmInit]]
