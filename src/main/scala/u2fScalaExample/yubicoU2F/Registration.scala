package u2fScalaExample.yubicoU2F

import com.yubico.u2f.U2F
import com.yubico.u2f.data.messages.{RegisterResponse, RegisterRequestData}
import com.yubico.u2f.exceptions.U2fBadInputException
import redis.clients.jedis.exceptions.JedisException
import u2fScalaExample.model._
import u2fScalaExample.storage.{UserStorage, RequestStorage}
import u2fScalaExample.Config.APP_ID

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}


object Registration {
  import cats.data.Xor
  import cats.syntax.xor._

  val u2f = new U2F()

  def start(username: Username)(implicit ec: ExecutionContext): Future[RegisterRequestData] = for {
      currentRegistrations <- UserStorage.getRegistrations(username)
      requestData <- Future(u2f.startRegistration(APP_ID, currentRegistrations.asJava))
      _ <- Future(RequestStorage.put(requestData.getRequestId, requestData.toJson))
    } yield requestData

  def finish(username: Username, tokenResponse: String)(implicit ec: ExecutionContext): Future[Xor[U2FError, Unit]] = {
    val reg = for {
      registerResponse <- Future(RegisterResponse.fromJson(tokenResponse))
      requestData <- popRequestData(registerResponse.getRequestId)
      registration <- Future(u2f.finishRegistration(requestData, registerResponse))
      _ <- UserStorage.put(username, registration.getKeyHandle, registration.toJson)
    } yield ().right

    reg.recover {
      case e: U2fBadInputException => U2FBadInputError().left
      case e: JedisException => JedisError().left
      case _ => UnexpectedError().left
    }
  }

  private def popRequestData(requestId: String)(implicit ec: ExecutionContext): Future[RegisterRequestData] = for {
    storedRequestData <- RequestStorage.get(requestId)
    _ <- RequestStorage.remove(requestId)
    requestData <- Future(RegisterRequestData.fromJson(storedRequestData))
  } yield requestData
}