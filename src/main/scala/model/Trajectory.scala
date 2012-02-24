package lila
package model

case class Trajectories(dirs: List[Direction], friends: Set[Pos], enemies: Set[Pos]) {

  def from(p: Pos): Set[Pos] = dirs flatMap { dir ⇒ forward(p, dir) } toSet

  private def forward(p: Pos, dir: Direction): List[Pos] = dir(p) match {
    case None                        ⇒ Nil
    case Some(next) if friends(next) ⇒ Nil
    case Some(next) if enemies(next) ⇒ List(next)
    case Some(next)                  ⇒ next :: forward(next, dir)
  }
}
