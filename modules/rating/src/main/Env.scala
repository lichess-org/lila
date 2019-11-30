package lila.rating

final class Env(settingStore: lila.memo.SettingStore.Builder) {

  import RatingFactor.implicits._

  val ratingFactorsSetting = settingStore[RatingFactors](
    "ratingFactor",
    default = Map.empty,
    text = "Rating gain factor per perf type".some
  )
}
