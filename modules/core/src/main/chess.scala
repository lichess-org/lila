package lila.core
package chess

opaque type Win = Boolean
object Win extends YesNo[Win]

opaque type Rank = Int
object Rank extends RelaxedOpaqueInt[Rank]

opaque type MultiPv = Int
object MultiPv extends RelaxedOpaqueInt[MultiPv]

opaque type Depth = Int
object Depth extends RelaxedOpaqueInt[Depth]
