package lila.app

import play.api.http.*
import play.api.mvc.Codec
import scalatags.Text.Frag

export lila.Lila.{ *, given }

given (using codec: Codec): ContentTypeOf[Frag] = ContentTypeOf(Some(ContentTypes.HTML))
given (using codec: Codec): Writeable[Frag]     = Writeable(frag => codec.encode(frag.render))
