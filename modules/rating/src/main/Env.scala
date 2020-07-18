package lila.rating

import com.softwaremill.macwire._

@Module
final class Env(settingStore: lila.memo.SettingStore.Builder) {

  import RatingFactor.implicits._

  lazy val ratingFactorsSetting = settingStore[RatingFactors](
    "ratingFactor",
    default = Map.empty,
    text = "Rating gain factor per perf type".some
  )

  val getFactors = ratingFactorsSetting.get _
}
