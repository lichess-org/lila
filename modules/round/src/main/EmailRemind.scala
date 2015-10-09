package lila.round

import org.joda.time.DateTime

case class EmailRemind(
  _id: String, // random
  playerId: String,
  userId: String,
  sent: Boolean,
  sendAt: DateTime,
  createdAt: DateTime)
