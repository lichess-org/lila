package lila.common

import org.specs2.mutable.Specification

class MultiKeyMapTest extends Specification {

  case class V(a: Int, b: Int)

  "MultiKeyMap.removed" should {
    val m = MultiKeyMap(Set(V(1, 100)))(_.a, _.b)
    "have entries" in {
      m.values == Set(V(1, 100))
    }
    "add a new entry" in {
      m.updated(V(2, 200)).values must_== Set(V(1, 100), V(2, 200))
    }
    "replace an entry" in {
      m.updated(V(1, 200)).values must_== Set(V(1, 200))
    }
    "remove empty entries" in {
      m.removed(Set.empty[V]).values must_== m.values
    }
    "remove entries" in {
      m.removed(Set(V(1, 100))).values must_== Set.empty
    }
    "expose keys" in {
      m.key1s.toSet must_== Set(1)
    }
  }
}
