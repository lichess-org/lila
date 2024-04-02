package lila.core
package playban

type BansOf = List[UserId] => Fu[Map[UserId, Int]]
