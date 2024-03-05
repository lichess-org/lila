package lila.lobby

/* Immutable map where each value has 2 independent keys.
 * Kind of like a DB entry with 2 *unique* indexes.
 * loosely inspired from https://github.com/joshlemer/MultiIndex
 */
final private class MultiKeyMap[K1, K2, V](index1: Map[K1, V], index2: Map[K2, V])(
    toK1: V => K1,
    toK2: V => K2
):

  export index1.{ values, size, contains, keys as key1s, get as get1 }

  def get2(k2: K2): Option[V] = index2.get(k2)

  def updated(toAdd: V): MultiKeyMap[K1, K2, V] =
    val (k1, k2) = (toK1(toAdd), toK2(toAdd))
    copy(
      index1 = index1.removed(k1).updated(k1, toAdd),
      index2 = index2.removed(k2).updated(k2, toAdd)
    )

  def removed(toRemove: V): MultiKeyMap[K1, K2, V] =
    val (k1, k2) = (toK1(toRemove), toK2(toRemove))
    copy(
      index1 = index1.removed(k1),
      index2 = index2.removed(k2)
    )

  def removed(toRemove: Set[V]): MultiKeyMap[K1, K2, V] =
    val (k1s, k2s) = (toRemove.map(toK1), toRemove.map(toK2))
    copy(
      index1 = index1 -- k1s,
      index2 = index2 -- k2s
    )

  def reset(newValues: Set[V]) = if newValues == values then this else MultiKeyMap(newValues)(toK1, toK2)

  private def copy(index1: Map[K1, V], index2: Map[K2, V]) =
    new MultiKeyMap(index1, index2)(toK1, toK2)

private object MultiKeyMap:

  def apply[K1, K2, V](values: Set[V])(toK1: V => K1, toK2: V => K2) =
    val (index1: Map[K1, V], index2: Map[K2, V]) =
      values.view.foldLeft((Map.empty[K1, V], Map.empty[K2, V])) { case ((i1, i2), value) =>
        (
          i1 + (toK1(value) -> value),
          i2 + (toK2(value) -> value)
        )
      }
    new MultiKeyMap[K1, K2, V](index1, index2)(toK1, toK2)
