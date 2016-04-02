package lila

package object relation extends PackageObject with WithPlay {

  type Relation = Boolean
  private[relation] val Follow: Relation = true
  private[relation] val Block: Relation = false

  private[relation] type ID = String
}
