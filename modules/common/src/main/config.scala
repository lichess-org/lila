package lila.common

import play.api.ConfigLoader

object config {

  implicit val maxPerPageLoader: ConfigLoader[MaxPerPage] =
    ConfigLoader(_.getInt).map(MaxPerPage.apply)

  implicit val collNameLoader: ConfigLoader[CollName] =
    ConfigLoader(_.getString).map(CollName.apply)

  implicit val SecretLoader: ConfigLoader[Secret] =
    ConfigLoader(_.getString).map(Secret.apply)
}
