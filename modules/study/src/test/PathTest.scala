package lila.study

import org.specs2.mutable._

class PathTest extends Specification {

  "intersect" >> {
    "nope" >> {
      Path("/?UE)8\\M(DYQDMTM'*`Y('aR-5").intersect(Path(")8VN")) === Path.root
    }
    "full" >> {
      val p = Path("/?UE)8\\M(DYQDMTM'*`Y('aR-5")
      p.intersect(p) === p
    }
    "partial left" >> {
      Path("/?UE)8\\M(DYQDMTM'*`Y('aR-5").intersect(Path("/?UE)8")) === Path("/?UE)8")
    }
    "partial right" >> {
      Path("/?UE)8").intersect(Path("/?UE)8\\M(DYQDMTM'*`Y('aR-5")) === Path("/?UE)8")
    }
  }
  "db" >> {
    val p = Path(
      """.>VF-=F=/?WG)8`<%.<.&.G>8>aP$5^W'#_b08UE>-\M(=]O=OXO.N[^NWMW&^`^*&^&5&aX,<E<-<OG&7XO%-PV-5VE5=E;<6;L=DLV6EVEDEYI19"""
    )
    "encoding" >> {
      Path(Path.decodeDbKey(Path.encodeDbKey(p).toString)) === p
    }
  }
  "utils" >> {
    val p = Path(
      """.>VF-=F=/?WG)8`<%.<.&.G>8>aP$5^W'#_b08UE>-\M(=]O=OXO.N[^NWMW&^`^*&^&5&aX,<E<-<OG&7XO%-PV-5VE5=E;<6;L=DLV6EVEDEYI19"""
    )
    p.depth === 57
  }
}
