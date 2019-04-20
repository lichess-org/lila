package lidraughts.rating

final class Env(settingStore: lidraughts.memo.SettingStore.Builder) {

  import RatingFactor.implicits._

  val ratingFactorsSetting = settingStore[RatingFactors](
    "ratingFactor",
    default = Map.empty,
    text = "Rating gain factor per perf type".some
  )
}

object Env {

  lazy val current: Env = "rating" boot new Env(
    settingStore = lidraughts.memo.Env.current.settingStore
  )
}
