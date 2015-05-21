/*
 * Copyright (c) Sourcy Software & Services GmbH 2015.
 *
 *     _____ ____   __  __ _____ _____ __  __    (_)____
 *    / ___// __ \ / / / // ___// ___// / / /   / // __ \
 *   (__  )/ /_/ // /_/ // /   / /__ / /_/ /_  / // /_/ /
 *  /____/ \____/ \__,_//_/    \___/ \__, /(_)/_/ \____/
 *                                  /____/
 *
 * Created by armin walland <a.walland@sourcy.io> on 2015-05-20.
 */

package io.sourcy.jirastatscollector

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl._


object NoSsl {
  private def trustAllCerts = Array[TrustManager] {
    new X509TrustManager() {
      override def getAcceptedIssuers: Array[X509Certificate] = null

      override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

      override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}
    }
  }

  def socketFactory: SSLSocketFactory = {
    val sc = SSLContext.getInstance("SSL")
    sc.init(null, trustAllCerts, new SecureRandom())
    sc.getSocketFactory
  }

  def hostVerifier: HostnameVerifier = new HostnameVerifier() {
    override def verify(s: String, sslSession: SSLSession): Boolean = true
  }
}
