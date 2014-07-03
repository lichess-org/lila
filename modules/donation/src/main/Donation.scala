package lila.donation

import org.joda.time.DateTime

case class Donation(
    _id: String, // random ID
    payPalTnx: Option[String], // PayPal transaction ID
    payPalSub: Option[String], // PayPal subscription ID
    userId: Option[String],
    email: Option[String],
    name: Option[String],
    date: DateTime,
    gross: Int, // $ cents
    fee: Int, // $ cents
    net: Int, // $ cents
    message: String,
    public: Boolean,
    publicAmount: Boolean) {

  def nonEmptyMessage = Some(message.trim) filter (_.nonEmpty)
}

object Donation {

  def make(
    payPalTnx: Option[String],
    payPalSub: Option[String],
    email: Option[String],
    name: Option[String],
    userId: Option[String],
    gross: Int,
    fee: Int,
    message: String) = Donation(
    _id = ornicar.scalalib.Random nextStringUppercase 8,
    payPalTnx = payPalTnx,
    payPalSub = payPalSub,
    email = email,
    name = name,
    userId = userId,
    date = DateTime.now,
    gross = gross,
    fee = fee,
    net = gross - fee,
    message = message,
    public = true,
    publicAmount = false)
}
