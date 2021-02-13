package lila

package object relation extends PackageObject {

  type Relation = Boolean
  val Follow: Relation = true
  val Block: Relation  = false

  private[relation] type ID = String

  private[relation] type OnlineStudyingCache = com.github.blemale.scaffeine.Cache[ID, String]
}
