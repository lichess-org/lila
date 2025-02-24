package lila.lobby

class MultiKeyMapTest extends munit.FunSuite:

  case class V(a: Int, b: Int)

  val m = MultiKeyMap(Set(V(1, 100)))(_.a, _.b)

  test("have entries"):
    assertEquals(m.values.toSet, Set(V(1, 100)))
  test("add a new entry"):
    assertEquals(m.updated(V(2, 200)).values.toSet, Set(V(1, 100), V(2, 200)))
  test("replace an entry"):
    assertEquals(m.updated(V(1, 200)).values.toSet, Set(V(1, 200)))
  test("remove empty entries"):
    assertEquals(m.removed(Set.empty[V]).values.toSet, m.values.toSet)
  test("remove entries"):
    assertEquals(m.removed(Set(V(1, 100))).values.toSet, Set.empty)
  test("expose keys"):
    assertEquals(m.key1s.toSet, Set(1))
