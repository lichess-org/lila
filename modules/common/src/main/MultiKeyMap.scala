package lila.common

/* Immutable map where each value has 2 independant keys.
 * Kind of like a DB entry with 2 *unique* indexes.
 * loosely inspired from https://github.com/joshlemer/MultiIndex
 */
final class MultiKeyMap[K1, K2, V](val values: Set[V], index1: Map[K1, V], index2: Map[K2, V])(
    toK1: V => K1,
    toK2: V => K2
) {

  def get1(k1: K1): Option[V] = index1 get k1

  def get2(k2: K2): Option[V] = index2 get k2

  def key1s: Iterable[K1] = index1.keys

  def updated(toAdd: V): MultiKeyMap[K1, K2, V] = {
    val (k1, k2) = (toK1(toAdd), toK2(toAdd))
    copy(
      values = values -- Set(get1(k1), get2(k2)).flatten + toAdd,
      index1 = index1.removed(k1).updated(k1, toAdd),
      index2 = index2.removed(k2).updated(k2, toAdd)
    )
  }

  def removed(toRemove: V): MultiKeyMap[K1, K2, V] = {
    val (k1, k2) = (toK1(toRemove), toK2(toRemove))
    copy(
      values = values -- Set(get1(k1), get2(k2)).flatten,
      index1 = index1.removed(k1),
      index2 = index2.removed(k2)
    )
  }

  def removed(toRemove: Set[V]): MultiKeyMap[K1, K2, V] = {
    val (k1s, k2s) = (toRemove map toK1, toRemove map toK2)
    copy(
      values = values -- k1s.flatMap(get1) -- k2s.flatMap(get2),
      index1 = index1 -- k1s,
      index2 = index2 -- k2s
    )
  }

  def size = values.size

  def reset(newValues: Set[V]) = if (newValues == values) this else MultiKeyMap(newValues)(toK1, toK2)

  private def copy(values: Set[V], index1: Map[K1, V], index2: Map[K2, V]) =
    new MultiKeyMap(values, index1, index2)(toK1, toK2)
}

object MultiKeyMap {

  def apply[K1, K2, V](values: Set[V])(toK1: V => K1, toK2: V => K2) = {
    val (index1: Map[K1, V], index2: Map[K2, V]) =
      values.view.foldLeft((Map.empty[K1, V], Map.empty[K2, V])) { case ((i1, i2), value) =>
        (
          i1 + (toK1(value) -> value),
          i2 + (toK2(value) -> value)
        )
      }
    new MultiKeyMap[K1, K2, V](values, index1, index2)(toK1, toK2)
  }
}
