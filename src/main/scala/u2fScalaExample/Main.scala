package u2fScalaExample

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directive1, Directive, Route}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.ActorMaterializer
import com.yubico.u2f.U2F
import com.yubico.u2f.data.DeviceRegistration
import com.yubico.u2f.data.messages.{AuthenticateRequestData, AuthenticateResponse, RegisterRequestData, RegisterResponse}

import collection.JavaConversions._
import collection.JavaConverters._

import u2fScalaExample.ssl.CustomSSLContext
import u2fScalaExample.Config.username
import u2fScalaExample.Config.password
import u2fScalaExample.Config.APP_ID
import u2fScalaExample.storage.{UserStorage, RequestStorage}


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

  val AuthGet: Directive1[String] =
    authenticateBasic(realm = "Basic Auth", confAuthenticator) & get

  val AuthPostWithTokenResponse: Directive[(String, String)] =
    authenticateBasic(realm = "Basic Auth", confAuthenticator) & post & formFields('tokenResponse)

  val route =
    Route.seal {
      path("authCheck") {
        AuthGet { username =>
          complete(s"The user is $username")
        }
      } ~
      path("startRegistration") {
        AuthGet { username =>
          val registerRequestData = u2f.startRegistration(APP_ID, getRegistrations(username).asJava)
          RequestStorage.put(registerRequestData.getRequestId, registerRequestData.toJson)
          complete {
            registerRequestData.toString + username
          }
        }
      } ~
      path("finishRegistration") {
        AuthPostWithTokenResponse { (username, tokenResponse) =>
          val registerResponse = RegisterResponse.fromJson(tokenResponse)

          val requestId = registerResponse.getRequestId
          val registerRequestData = RegisterRequestData.fromJson(requestId)
          RequestStorage.remove(registerResponse.getRequestId)
          val registration = u2f.finishRegistration(registerRequestData, registerResponse)
          UserStorage.put(username, registration.getKeyHandle, registration.toJson)

          complete { "Registration completed" }
        }
      } ~
      path("startAuthentication") {
        AuthGet { username =>
          val authenticateRequestData = u2f.startAuthentication(APP_ID, getRegistrations(username).asJava)
          RequestStorage.put(authenticateRequestData.getRequestId, authenticateRequestData.toJson)
          complete { authenticateRequestData.toJson + username }
        }
      } ~
      path("finishAuthentication") {
        AuthPostWithTokenResponse { (username, tokenResponse) =>
          val authenticateResponse = AuthenticateResponse.fromJson(tokenResponse)
          val requestId = authenticateResponse.getRequestId
          val authenticateRequest = AuthenticateRequestData.fromJson(requestId)

          RequestStorage.remove(requestId)

          val deviceRegistration = u2f.finishAuthentication(authenticateRequest, authenticateResponse, getRegistrations(username).asJava)
          complete { "Authentication completed" + deviceRegistration.toString }
        }
      }
    }

  private def getRegistrations(username: String): Iterable[DeviceRegistration] = {
    val registrations = UserStorage.get(username).values()
    registrations.map(reg => DeviceRegistration.fromJson(reg))
  }

  Http().bindAndHandle(route, "localhost", 8080, httpsContext = Some(httpsContext))
    .recover { case e: Throwable ⇒ system.shutdown() }
}
