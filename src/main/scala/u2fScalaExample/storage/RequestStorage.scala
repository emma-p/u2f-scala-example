package u2fScalaExample.storage

import java.lang.Long
import java.util

import redis.clients.jedis.Jedis
import collection.JavaConversions._

object RequestStorage {
  def get(requestId: String): String = {
    JedisClient.store.get(s"requestStorage:$requestId")
  }

  def put(requestId: String, requestData: String): String = {
    JedisClient.store.set(s"requestStorage:$requestId", requestData)
  }
  def remove(requestId: String): Long = {
    JedisClient.store.del(s"requestStorage:$requestId")
  }
}

object UserStorage {
  def put(username: String, keyHandle: String, registration: String): String = {
    JedisClient.store.hmset(s"userStorage:$username", Map(keyHandle -> keyHandle, registration -> registration))
  }

  def get(username: String): util.Map[String, String] = {
    JedisClient.store.hgetAll(s"userStorage: $username")
  }
}

object JedisClient {
  lazy val store: Jedis = new Jedis("localhost")
}
