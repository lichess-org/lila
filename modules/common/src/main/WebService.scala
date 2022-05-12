package lila.common

object WebService {

  def fixInput(in: Seq[(String, Any)]): Seq[(String, String)] =
    in flatMap {
      case (name, Some(x)) => Some(name -> x.toString)
      case (_, None)       => None
      case (name, x)       => Some(name -> x.toString)
    }

  def debugInput(data: Seq[(String, Any)]) =
    fixInput(data) map { case (k, v) => s"$k=$v" } mkString " "
}
