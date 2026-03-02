package lila.study

case class MoveOpts(
    sticky: Boolean,
    promoteToMainline: Boolean
)

object MoveOpts:

  import play.api.libs.json.*
  import play.api.libs.functional.syntax.*

  private val default = MoveOpts(
    sticky = true,
    promoteToMainline = false
  )

  def parse(o: JsObject): MoveOpts = (o \ "d").asOpt[MoveOpts] | default

  private given Reads[MoveOpts] = (
    (__ \ "sticky")
      .readNullable[Boolean]
      .map(_ | default.sticky)
      .and((__ \ "promote").readNullable[Boolean].map(_ | default.promoteToMainline))
  )(MoveOpts.apply)
