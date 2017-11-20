package lds

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter

import com.typesafe.scalalogging.Logger
import lds.from.prototypes.LambdaRuntimeProperties

class WebhookSecretChecker {

  val logger = Logger(classOf[WebhookSecretChecker])

  def checkSignature(payload: String, ghSignature: String): Boolean = {
    val secret = new SecretKeySpec(LambdaRuntimeProperties.webhookSecretKey.getBytes(), "HmacSHA1")
    val hmac = Mac.getInstance("HmacSHA1")
    hmac.init(secret)

    val sig = hmac.doFinal(payload.getBytes("UTF-8"))
    val hashOfPayload = s"sha1=${DatatypeConverter.printHexBinary(sig)}"
    logger.info("hashOfPayload:" + hashOfPayload.toLowerCase())
    logger.info("hashFromGH:" + ghSignature.toLowerCase())
    ghSignature.equalsIgnoreCase(hashOfPayload)

  }
}
