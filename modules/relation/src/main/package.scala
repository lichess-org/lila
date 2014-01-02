package lila

import lila.db.JsTube

package object relation extends PackageObject with WithPlay {

  type Relation = Boolean
  private[relation] val Follow: Relation = true
  private[relation] val Block: Relation = false

  private[relation] type ID = String
  private[relation] type Username = String
  private[relation] type User = (ID, Username)

  object tube {

    private[relation] implicit lazy val relationTube =
      JsTube.json inColl Env.current.relationColl
  }
}
