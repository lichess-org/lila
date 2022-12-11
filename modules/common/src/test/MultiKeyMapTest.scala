package lila.common

import org.specs2.mutable.Specification

class MultiKeyMapTest extends Specification {

  case class V(a: Int, b: Int)

  "MultiKeyMap.removed" >> {
    val m = MultiKeyMap(Set(V(1, 100)))(_.a, _.b)
    "have entries" >> {
      m.values.toSet == Set(V(1, 100))
    }
    "add a new entry" >> {
      m.updated(V(2, 200)).values.toSet === Set(V(1, 100), V(2, 200))
    }
    "replace an entry" >> {
      m.updated(V(1, 200)).values.toSet === Set(V(1, 200))
    }
    "remove empty entries" >> {
      m.removed(Set.empty[V]).values.toSet === m.values.toSet
    }
    "remove entries" >> {
      m.removed(Set(V(1, 100))).values.toSet === Set.empty
    }
    "expose keys" >> {
      m.key1s.toSet === Set(1)
    }
  }
}
