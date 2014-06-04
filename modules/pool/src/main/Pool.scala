package lila.pool

case class Pool(
  id: ID, // also a slug
  name: String,
  clockLimit: Int, // seconds
  clockIncrement: Int, // seconds
  variant: chess.Variant)
