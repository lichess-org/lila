import play.api.libs.json._
import play.api.libs.functional.syntax._

object Bar {

  case class Foo(a: Int, b: String)

  object Foo // without this line, the program compiles

  implicit val jsonReads = (
    (__ \ 'a).read[Int] and
    (__ \ 'b).read[String] 
  )(Foo.apply _)
}
