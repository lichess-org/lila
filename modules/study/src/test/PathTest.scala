package lila.study

import chess.format.UciPath
import org.specs2.mutable._

class UciPathTest extends Specification {
  "db" >> {
    val p = UciPath(
      """.>VF-=F=/?WG)8`<%.<.&.G>8>aP$5^W'#_b08UE>-\M(=]O=OXO.N[^NWMW&^`^*&^&5&aX,<E<-<OG&7XO%-PV-5VE5=E;<6;L=DLV6EVEDEYI19"""
    )
    "encoding" >> {
      UciPathDb.decodeDbKey(UciPathDb.encodeDbKey(p).toString) === p
    }
  }
}
