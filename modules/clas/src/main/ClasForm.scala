package lila.clas

import play.api.data._
import play.api.data.Forms._

final class ClasForm {

  import ClasForm._

  val form = Form(
    mapping(
      "name" -> text(minLength = 3, maxLength = 100),
      "desc" -> text(minLength = 0, maxLength = 2000)
    )(Data.apply)(Data.unapply)
  )

  def create = form

  def edit(c: Clas) = form fill Data(
    name = c.name,
    desc = c.desc
  )
}

object ClasForm {

  case class Data(
      name: String,
      desc: String
  )
}
