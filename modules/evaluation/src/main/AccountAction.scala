package lila.evaluation

sealed trait AccountAction {
  val description: String
  val colorClass: String
  override def toString = description
}

object AccountAction {
  case object EngineAndBan extends AccountAction {
    val description: String = "Mark and IP ban"
    val colorClass = "4"
  }
  case object Engine extends AccountAction {
    val description: String = "Mark as engine"
    val colorClass = "3"
  }
  case object Report extends AccountAction {
    val description: String = "Report to mods"
    val colorClass = "2"
  }
  case object Nothing extends AccountAction {
    val description: String = "Not suspicious"
    val colorClass = "1"
  }
}
