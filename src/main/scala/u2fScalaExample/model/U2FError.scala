package u2fScalaExample.model

sealed trait U2FError {
  def get: String
}

case class U2FBadInputError() extends U2FError {
  override def get = "U2f Bad Input Error"
}

case class DeviceCompromisedError() extends U2FError {
  override def get = "Device compromised"
}

case class JedisError() extends U2FError {
  override def get = "There was a problem with Jedis"
}

case class UnexpectedError(get: String) extends U2FError
