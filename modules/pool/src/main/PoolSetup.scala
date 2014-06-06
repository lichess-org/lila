package lila.pool

case class PoolSetup(
  id: ID, // also a slug
  name: String,
  clockLimit: Int, // seconds
  clockIncrement: Int, // seconds
  variant: chess.Variant)
