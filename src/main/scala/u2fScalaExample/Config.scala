package u2fScalaExample

import com.typesafe.config.ConfigFactory

object Config {
  val conf = ConfigFactory.load

  val username = conf.getString("auth.username")
  val password = conf.getString("auth.password")
  val keystorePath = conf.getString("ssl.keystore.path")
  val keystorePassword = conf.getString("ssl.keystore.password")
  val APP_ID = conf.getString("appId")
}
