/*
 * Copyright (c) Sourcy Software & Services GmbH 2015.
 *
 *     _____ ____   __  __ _____ _____ __  __    (_)____
 *    / ___// __ \ / / / // ___// ___// / / /   / // __ \
 *   (__  )/ /_/ // /_/ // /   / /__ / /_/ /_  / // /_/ /
 *  /____/ \____/ \__,_//_/    \___/ \__, /(_)/_/ \____/
 *                                  /____/
 *
 * Created by armin walland <a.walland@sourcy.io> on 2015-05-22.
 */

package io.sourcy.jirastatscollector

import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl._

import net.liftweb.json._

import scala.io.Source

object JiraClient {
  private def runJql(jql: String): JValue = {
    HttpsURLConnection.setDefaultSSLSocketFactory(NoSsl.socketFactory)
    HttpsURLConnection.setDefaultHostnameVerifier(NoSsl.hostVerifier)
    val connection = new URL(Settings.jiraUrl + "jql=" + jql).openConnection
    connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION, HttpBasicAuth.getHeader(Settings.jiraUser, Settings.jiraPassword))
    parse(Source.fromInputStream(connection.getInputStream).mkString)
  }

  def extractChildIssues(epic: String): List[String] = {
    val values = (runJql(Settings.epicCustomField + "=%s".format(epic)) \ "issues" \ "key").values
    epic :: values.asInstanceOf[List[(String, String)]].map(tuple => tuple._2)
  }

  def extractSubTasks(issue: String): List[String] = {
    val values = (runJql("parent=%s".format(issue)) \ "issues" \ "key").values
    var ret: List[String] = List()
    try {
      ret = values.asInstanceOf[List[(String, String)]].map(tuple => tuple._2)
    } catch {
      case e: ClassCastException =>
    }
    issue :: ret
  }
}

private object NoSsl {
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
