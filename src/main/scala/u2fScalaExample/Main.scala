package u2fScalaExample

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.{StatusCodes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{Directive, Directive1, Route}
import akka.stream.ActorMaterializer
import cats.data.Xor
import com.yubico.u2f.U2F
import u2fScalaExample.Config.{password, username}
import u2fScalaExample.marshallers.PlayTwirlMarshaller.twirlHtmlMarshaller
import u2fScalaExample.model.Username
import u2fScalaExample.ssl.CustomSSLContext

import u2fScalaExample.yubicoU2F.{Authentication, Registration}

import scala.util.{Failure, Success}

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

  //TODO check why returns username
  val auth = authenticateBasic(realm = "Basic Auth", confAuthenticator).map(name => Username(name))

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
          onComplete(Registration.start(username)) {
            case Success(data) =>
              complete { HttpEntity(`application/json`, data.toJson) }
            case Failure(error) =>
              complete { StatusCodes.InternalServerError }
          }
        }
      } ~
      path("finishRegistration") {
        authPostWithTokenResponse { (username, tokenResponse) =>
          onComplete(Registration.finish(username, tokenResponse)) {
            case Success(Xor.Right(())) =>
              complete { "Registration completed" }
            //TODO error handling
            case Success(Xor.Left((e))) =>
              complete { StatusCodes.InternalServerError }
            case _ =>
              complete { StatusCodes.InternalServerError }
          }
        }
      } ~
      path("startAuthentication") {
        authGet { username =>
          onComplete(Authentication.start(username)) {
            case Success(data) =>
              complete { HttpEntity(`application/json`, data.toJson) }
            case Failure(error) =>
              complete { StatusCodes.InternalServerError }
          }
        }
      } ~
      path("finishAuthentication") {
        authPostWithTokenResponse { (username, tokenResponse) =>
          onComplete(Authentication.finish(username, tokenResponse)) {
            case Success(Xor.Right(deviceRegistration)) =>
              complete { "Authentication completed" + deviceRegistration.toString }
              //TODO error handling
            case Success(Xor.Left(e)) =>
              complete { StatusCodes.InternalServerError }
            case _ =>
              complete { StatusCodes.InternalServerError }
          }
        }
      } ~
      getFromDirectory("public")
    }


  Http().bindAndHandle(route, "localhost", 8080, httpsContext = Some(httpsContext))
    .recover { case e: Throwable ⇒ system.shutdown() }
}
