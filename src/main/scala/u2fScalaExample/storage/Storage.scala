package u2fScalaExample.storage

import java.lang.Long
import java.util

import com.yubico.u2f.data.DeviceRegistration
import redis.clients.jedis.Jedis
import u2fScalaExample.model.Username
import collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

object RequestStorage {
  def get(requestId: String)(implicit ec: ExecutionContext): Future[String] = {
    Future(JedisClient.store.get(s"requestStorage:$requestId"))
  }

  def put(requestId: String, requestData: String)(implicit ec: ExecutionContext): Future[String] = {
    Future(JedisClient.store.set(s"requestStorage:$requestId", requestData))
  }

  def remove(requestId: String)(implicit ec: ExecutionContext): Future[Long] = {
    Future(JedisClient.store.del(s"requestStorage:$requestId"))
  }
}

object UserStorage {
  def getRegistrations(username: Username)(implicit ec: ExecutionContext): Future[Iterable[DeviceRegistration]] = {
    get(username).map( res =>
      res.values().map(reg => DeviceRegistration.fromJson(reg))
    )
  }

  def put(username: Username, keyHandle: String, registration: String)(implicit ec: ExecutionContext): Future[String] = {
    Future(JedisClient.store.hmset(s"userStorage:${username.get}", Map(keyHandle -> keyHandle, registration -> registration)))
  }

  private def get(username: Username)(implicit ec: ExecutionContext): Future[util.Map[String, String]] = {
    Future(JedisClient.store.hgetAll(s"userStorage: ${username.get}"))
  }

}

object JedisClient {
  lazy val store: Jedis = new Jedis("localhost")
}
