package u2fScalaExample

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{Directive, Directive1, Route}
import akka.stream.ActorMaterializer
import com.yubico.u2f.U2F
import com.yubico.u2f.data.DeviceRegistration
import com.yubico.u2f.data.messages.{AuthenticateRequestData, AuthenticateResponse, RegisterRequestData, RegisterResponse}
import u2fScalaExample.Config.{APP_ID, password, username}
import u2fScalaExample.marshallers.PlayTwirlMarshaller.twirlHtmlMarshaller
import u2fScalaExample.ssl.CustomSSLContext
import u2fScalaExample.storage.{RequestStorage, UserStorage}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

object Main extends App with CustomSSLContext {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  val u2f = new U2F()

  def confAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if id == username && p.verify(password) => Some(id)
      case _ => None
    }
  }

  val auth = authenticateBasic(realm = "Basic Auth", confAuthenticator)

  val authGet: Directive1[String] =
    auth & get

  val authPostWithTokenResponse: Directive[(String, String)] =
    auth & post & formFields('tokenResponse)

  val route =
    Route.seal {
      path("settings") {

        authGet { username =>
          complete { html.settings.render(username) }
        }
      } ~
      path("register") {
        (auth & post) { username =>
          val registerRequestData = u2f.startRegistration(APP_ID, getRegistrations(username).asJava)
          RequestStorage.put(registerRequestData.getRequestId, registerRequestData.toJson)

          complete {
            HttpEntity(`application/json`, registerRequestData.toJson)
          }
        }
      } ~
      path("finishRegistration") {
        authPostWithTokenResponse { (username, tokenResponse) =>
          val registerResponse = RegisterResponse.fromJson(tokenResponse)

          val requestData = RequestStorage.get(registerResponse.getRequestId)
          RequestStorage.remove(registerResponse.getRequestId)

          val registerRequestData = RegisterRequestData.fromJson(requestData)

          val registration = u2f.finishRegistration(registerRequestData, registerResponse)
          UserStorage.put(username, registration.getKeyHandle, registration.toJson)

          complete { "Registration completed" }
        }
      } ~
      path("startAuthentication") {
        authGet { username =>
          val authenticateRequestData = u2f.startAuthentication(APP_ID, getRegistrations(username).asJava)
          RequestStorage.put(authenticateRequestData.getRequestId, authenticateRequestData.toJson)

          complete { authenticateRequestData.toJson + username }
        }
      } ~
      path("finishAuthentication") {
        authPostWithTokenResponse { (username, tokenResponse) =>
          val authenticateResponse = AuthenticateResponse.fromJson(tokenResponse)
          val requestId = authenticateResponse.getRequestId
          val authenticateRequest = AuthenticateRequestData.fromJson(requestId)

          RequestStorage.remove(requestId)

          val deviceRegistration = u2f.finishAuthentication(authenticateRequest, authenticateResponse, getRegistrations(username).asJava)

          complete { "Authentication completed" + deviceRegistration.toString }
        }
      } ~ getFromDirectory("public")
    }

  private def getRegistrations(username: String): Iterable[DeviceRegistration] = {
    val registrations = UserStorage.get(username).values()
    registrations.map(reg => DeviceRegistration.fromJson(reg))
  }

  Http().bindAndHandle(route, "localhost", 8080, httpsContext = Some(httpsContext))
    .recover { case e: Throwable ⇒ system.shutdown() }
}
