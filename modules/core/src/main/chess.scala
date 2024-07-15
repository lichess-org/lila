package lila.core
package chess

opaque type Win = Boolean
object Win extends YesNo[Win]

opaque type Rank = Int
object Rank extends OpaqueInt[Rank]

opaque type MultiPv = Int
object MultiPv extends OpaqueInt[MultiPv]

opaque type Depth = Int
object Depth extends OpaqueInt[Depth]
