package u2fScalaExample.yubicoU2F

import com.yubico.u2f.U2F
import com.yubico.u2f.data.DeviceRegistration
import com.yubico.u2f.data.messages.{AuthenticateResponse, AuthenticateRequestData}
import com.yubico.u2f.exceptions.{U2fBadInputException, DeviceCompromisedException}
import u2fScalaExample.model._
import u2fScalaExample.storage.{UserStorage, RequestStorage}
import u2fScalaExample.Config.APP_ID

import scala.collection.JavaConverters._
import scala.concurrent.{Future, ExecutionContext}


object Authentication {
  import cats.data.Xor
  import cats.syntax.xor._

  val u2f = new U2F()

  def start(username: Username)(implicit ec: ExecutionContext): Future[AuthenticateRequestData] = for {
      currentRegistrations <- UserStorage.getRegistrations(username)
      requestData <- Future(u2f.startAuthentication(APP_ID, currentRegistrations.asJava))
      _ <- RequestStorage.put(requestData.getRequestId, requestData.toJson)
    } yield requestData

  def finish(username: Username, tokenResponse: String)(implicit ec: ExecutionContext): Future[Xor[U2FError, DeviceRegistration]] = {
    val auth = for {
      currentRegistrations <- UserStorage.getRegistrations(username)
      response <- Future(AuthenticateResponse.fromJson(tokenResponse))
      requestData <- popRequestData(response.getRequestId)
      deviceRegistration <- Future(u2f.finishAuthentication(requestData, response, currentRegistrations.asJava))
    } yield deviceRegistration.right

    auth.recover {
      case e: U2fBadInputException => U2FBadInputError().left
      case e: DeviceCompromisedException => DeviceCompromisedError().left
    }
  }

  private def popRequestData(requestId: String)(implicit ec: ExecutionContext): Future[AuthenticateRequestData] = for {
    storedRequestData <- RequestStorage.get(requestId)
    _ <- RequestStorage.remove(requestId)
    requestData <- Future(AuthenticateRequestData.fromJson(storedRequestData))
  } yield requestData

}
