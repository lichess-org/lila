package lila.donation

import org.joda.time.DateTime

case class Donation(
    _id: String, // random ID
    payPalTnx: Option[String], // PayPal transaction ID
    userId: Option[String],
    email: Option[String],
    name: Option[String],
    date: DateTime,
    amount: Int, // $ cents
    message: String,
    public: Boolean,
    publicAmount: Boolean) {

  def nonEmptyMessage = Some(message.trim) filter (_.nonEmpty)

  def afterFees = Donation afterFees amount
}

object Donation {

  def make(
    payPalTnx: Option[String],
    email: Option[String],
    name: Option[String],
    userId: Option[String],
    amount: Int,
    message: String) = Donation(
    _id = ornicar.scalalib.Random nextStringUppercase 8,
    payPalTnx = payPalTnx,
    email = email,
    name = name,
    userId = userId,
    date = DateTime.now,
    amount = amount,
    message = message,
    public = true,
    publicAmount = false)

  def afterFees(amount: Int) = math.max(0, math.round(
    amount * 0.966f - 30
  ))
}
