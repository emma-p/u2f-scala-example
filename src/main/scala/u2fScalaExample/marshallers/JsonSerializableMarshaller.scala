package u2fScalaExample.marshallers

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import com.yubico.u2f.data.messages.json.JsonSerializable
import akka.http.scaladsl.model.ContentTypes.`application/json`

object JsonSerializableMarshaller {
  implicit val jsonSerializableMarshaller: ToEntityMarshaller[JsonSerializable] =
    Marshaller.StringMarshaller.wrap(`application/json`)(_.toJson)
}
