package u2fScalaExample.ssl

import akka.http.scaladsl.HttpsContext
import org.apache.camel.util.jsse.{SSLContextParameters, KeyManagersParameters, KeyStoreParameters}
import u2fScalaExample.Config.{keystorePassword, keystorePath}

import scala.collection.immutable

trait CustomSSLContext {
  private def sslContext = {
    val keyStoreResource = keystorePath

    val ksp = new KeyStoreParameters()
    ksp.setResource(keyStoreResource)
    ksp.setPassword(keystorePassword)

    val kmp = new KeyManagersParameters()
    kmp.setKeyStore(ksp)
    kmp.setKeyPassword(keystorePassword)

    val scp = new SSLContextParameters()
    scp.setKeyManagers(kmp)

    val context= scp.createSSLContext()

    context
  }

  val httpsContext = HttpsContext(sslContext = sslContext,
    enabledCipherSuites = Some(immutable.Seq("TLS_RSA_WITH_AES_256_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA")),
    enabledProtocols = Some(immutable.Seq("SSLv3", "TLSv1"))
  )
}
