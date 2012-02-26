package lila
package repo

import com.novus.salat.annotations._
import com.mongodb.casbah.Imports._

case class Player(

  id: String,

  color: String,

  ps: String
)
