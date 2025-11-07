package lila.web

import play.api.libs.json.Json

final class Mobile(lilaVersion: Option[WebConfig.LilaVersion], settingStore: lila.memo.SettingStore.Builder):

  val androidVersion = settingStore[String](
    "mobileAndroidVersion",
    default = "0.17.15",
    text = "Mobile Android version to recommend upgrading to".some
  )
  val iosVersion = settingStore[String](
    "mobileIosVersion",
    default = "0.17.15",
    text = "Mobile iOS version to recommend upgrading to".some
  )

  def json = Json.obj(
    "lila" -> lilaVersion.fold("dev")(_.date),
    "android" -> androidVersion.get(),
    "ios" -> iosVersion.get()
  )
