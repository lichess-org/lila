package lila

import lila.db.Tube

package object relation extends PackageObject with WithPlay {

  private[relation] type ID = String

  type Relation = Boolean
  val Follow: Relation = true
  val Block: Relation = false

  object tube {

    private[relation] implicit lazy val relationTube =
      Tube.json inColl Env.current.relationColl
  }
}
