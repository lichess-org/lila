package lila.app
package mod

final class ModlogRepo(db: LilaDB, name: String)
    extends mongodb.Coll[Modlog](db, name, Modlog.json) {

  def recent(nb: Int): Fu[List[Modlog]] = find(query.sort(sortNaturalDesc), nb)
}
