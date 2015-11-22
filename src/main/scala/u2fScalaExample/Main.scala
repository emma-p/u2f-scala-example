package u2fScalaExample

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.ActorMaterializer

import u2fScalaExample.ssl.CustomSSLContext
import u2fScalaExample.Config.username
import u2fScalaExample.Config.password

object Main extends App with CustomSSLContext {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  def confAuthenticator(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if id == username && p.verify(password) => Some(id)
      case _ => None
    }
  }

  val route =
    Route.seal {
      path("hello") {
        authenticateBasic(realm = "Basic Auth", confAuthenticator) { username =>
          complete(s"The user is $username")
        }
      }
    }

  Http().bindAndHandle(route, "localhost", 8080, httpsContext = Some(httpsContext))
    .recover { case e: Throwable ⇒ system.shutdown() }
}
