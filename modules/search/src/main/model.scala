package lila.search

import alleycats.Zero

opaque type From = Int
object From extends OpaqueInt[From]

opaque type Size = Int
object Size extends OpaqueInt[Size]

object Date:
  val format    = "yyyy-MM-dd HH:mm:ss"
  val formatter = java.time.format.DateTimeFormatter.ofPattern(format)
