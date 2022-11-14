package lila

import play.api.http.*
import play.api.mvc.Codec
import scalatags.Text.Frag

package object app extends PackageObject:

  given (using codec: Codec): ContentTypeOf[Frag] = ContentTypeOf(Some(ContentTypes.HTML))

  given (using codec: Codec): Writeable[Frag] = Writeable(frag => codec.encode(frag.render))
