package lila.plan

private object WebService:

  def fixInput(in: Seq[(String, Matchable)]): Seq[(String, String)] =
    in.flatMap:
      case (name, Some(x)) => Some(name -> x.toString)
      case (_, None) => None
      case (name, x) => Some(name -> x.toString)

  def debugInput(data: Seq[(String, Matchable)]) =
    fixInput(data).map { case (k, v) => s"$k=$v" }.mkString(" ")
