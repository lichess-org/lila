package lila.core
package playban

opaque type RageSit = Int
object RageSit extends OpaqueInt[RageSit]:
  extension (a: RageSit) def counterView = a.value / 10

type BansOf        = List[UserId] => Fu[Map[UserId, Int]]
type RageSitOf     = UserId => Fu[RageSit]
type HasCurrentPlayban = UserId => Fu[Boolean]
