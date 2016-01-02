package u2fScalaExample.model

sealed trait U2FError

case class U2FBadInputError() extends U2FError {
  def get(): String = "U2f Bad Input Error"
}

case class DeviceCompromisedError() extends U2FError {
  def get(): String = "??? Device compromised"
}

case class JedisError() extends U2FError {
  def get(): String = "There was a problem with Jedis"
}

case class UnexpectedError() extends U2FError
