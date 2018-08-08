package lidraughts

package object forum extends PackageObject {

  private[forum] def teamSlug(id: String) = s"team-$id"

  private[forum] val logger = lidraughts.log("forum")
}
