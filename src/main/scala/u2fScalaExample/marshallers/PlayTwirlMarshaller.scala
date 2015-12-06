package u2fScalaExample.marshallers

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.MediaTypes._
import play.twirl.api._

object PlayTwirlMarshaller {

  implicit val twirlHtmlMarshaller = twirlMarshaller[Html](`text/html`)

  protected def twirlMarshaller[A <: AnyRef: Manifest](contentType: ContentType): ToEntityMarshaller[A] =
    Marshaller.StringMarshaller.wrap(contentType)(_.toString)
}
