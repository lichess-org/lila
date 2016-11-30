package lila.pool

import org.joda.time.DateTime

case class PoolMember(
  userId: String,
  rating: Int,
  createdAt: DateTime)
