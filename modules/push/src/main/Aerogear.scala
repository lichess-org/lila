package lila.push

import lila.user.User

final class Aerogear(config: Aerogear.Config) {

  def register(user: User, deviceId: String): Funit = ???
}

private object Aerogear {

  case class Config(
    url: String,
    variantId: String,
    secret: String)
}
