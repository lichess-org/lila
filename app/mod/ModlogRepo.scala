package lila
package mod

final class ModlogRepo(db: LilaDB, name: String)
    extends Coll[Modlog](db, name, Modlog.json) {
}
