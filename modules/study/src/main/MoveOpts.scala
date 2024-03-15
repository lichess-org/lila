package lila.study

case class MoveOpts(
    write: Boolean,
    sticky: Boolean,
    promoteToMainline: Boolean
)

object MoveOpts:

  import play.api.libs.json.*
  import play.api.libs.functional.syntax.*

  private val default = MoveOpts(
    write = true,
    sticky = true,
    promoteToMainline = false
  )

  def parse(o: JsObject): MoveOpts = (o \ "d").asOpt[MoveOpts] | default

  private given Reads[MoveOpts] = (
    (__ \ "write")
      .readNullable[Boolean]
      .map(_ | default.write)
      .and((__ \ "sticky").readNullable[Boolean].map(_ | default.sticky))
      .and((__ \ "promote").readNullable[Boolean].map(_ | default.promoteToMainline))
  )(MoveOpts.apply)
