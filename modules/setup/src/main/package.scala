package lila.setup

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val variantsWhereWhiteIsBetter: Set[chess.variant.Variant] = Set(
  chess.variant.ThreeCheck,
  chess.variant.Atomic,
  chess.variant.Horde,
  chess.variant.RacingKings,
  chess.variant.Antichess
)
