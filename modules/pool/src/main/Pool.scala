package lila.pool

import lila.user.User

case class Pool(
  setup: PoolSetup,
  users: List[User])
