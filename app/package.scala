package lila

import play.api.http._
import play.api.mvc.Codec
import scalatags.Text.Frag

package object app extends PackageObject {

  given (using codec: Codec): ContentTypeOf[Frag] = ContentTypeOf(Some(ContentTypes.HTML))

  given (using codec: Codec): Writeable[Frag] = Writeable(frag => codec.encode(frag.render))
}
