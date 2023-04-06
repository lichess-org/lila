package lila.push

private enum Urgency(val key: String):

  case VeryLow extends Urgency("very-low") // Delay delivery to conserve battery
  case Low     extends Urgency("low")
  case Normal  extends Urgency("normal")
  case High    extends Urgency("high")
