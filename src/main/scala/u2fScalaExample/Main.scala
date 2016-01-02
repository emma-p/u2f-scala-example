package u2fScalaExample

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{Directive, Directive1, Route}
import akka.stream.ActorMaterializer
import cats.data.Xor
import com.yubico.u2f.U2F
import u2fScalaExample.Config.{password, username}
import u2fScalaExample.marshallers.JsonSerializableMarshaller._
import u2fScalaExample.marshallers.PlayTwirlMarshaller.twirlHtmlMarshaller
import u2fScalaExample.model.Username
import u2fScalaExample.ssl.CustomSSLContext
import u2fScalaExample.yubicoU2F.{Authentication, Registration}

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

  val auth = authenticateBasic(realm = "Basic Auth", confAuthenticator).map(Username)

  val authGet: Directive1[Username] =
    auth & get

  val authPostWithTokenResponse: Directive[(Username, String)] =
    auth & post & formFields('tokenResponse)

  val route =
    Route.seal {
      path("settings") {

        authGet { username =>
          complete { html.settings.render(username.get) }
        }
      } ~
      path("register") {
        (auth & post) { username =>
          onSuccess(Registration.start(username))(complete(_))
        }
      } ~
      path("finishRegistration") {
        authPostWithTokenResponse { (username, tokenResponse) =>
          onSuccess(Registration.finish(username, tokenResponse)) {
            case Xor.Right(_) =>
              complete { "Registration completed" }
            case Xor.Left(e) =>
              complete { (StatusCodes.BadRequest, e.get) }
          }
        }
      } ~
      path("startAuthentication") {
        authGet { username =>
          onSuccess(Authentication.start(username))(complete(_))
        }
      } ~
      path("finishAuthentication") {
        authPostWithTokenResponse { (username, tokenResponse) =>
          onSuccess(Authentication.finish(username, tokenResponse)) {
            case Xor.Right(deviceRegistration) =>
              complete { "Authentication completed" + deviceRegistration.toString }
            case Xor.Left(e) =>
              complete { (StatusCodes.BadRequest, e.get) }
          }
        }
      } ~
      getFromDirectory("public")
    }

  Http().bindAndHandle(route, "localhost", 8080, httpsContext = Some(httpsContext))
    .recover { case e: Throwable ⇒ system.shutdown() }
}
