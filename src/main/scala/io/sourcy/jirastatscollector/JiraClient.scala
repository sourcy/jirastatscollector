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

  def extractAllIssues(epics: Seq[String]): Seq[String] =
    epics.flatMap(epic => extractChildIssues(epic).flatMap(issue => extractSubTasks(issue)))

  private def runJql(jql: String): JValue = {
    NoSsl.disableSslChecking()
    val connection = new URL(Settings.jiraUrl + "jql=%s".format(jql)).openConnection
    connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION, HttpBasicAuth.getHeader(Settings.jiraUser, Settings.jiraPassword))
    parse(Source.fromInputStream(connection.getInputStream).mkString)
  }

  private def extractChildIssues(epic: String): List[String] =
    epic :: extractIssuesFromJValue((runJql(Settings.epicCustomField + "=%s".format(epic)) \ "issues" \ "key").values)

  private def extractIssuesFromJValue(values: JsonAST.JValue#Values): List[String] =
    values.asInstanceOf[List[(String, String)]].map(tuple => tuple._2)

  private def extractSubTasks(issue: String): List[String] = {
    val values = (runJql("parent=%s".format(issue)) \ "issues" \ "key").values
    var ret: List[String] = List()
    try {
      ret = extractIssuesFromJValue(values)
    } catch {
      case e: ClassCastException =>
    }
    issue :: ret
  }
}

private object NoSsl {
  def disableSslChecking(): Unit = {
    HttpsURLConnection.setDefaultSSLSocketFactory(NoSsl.socketFactory)
    HttpsURLConnection.setDefaultHostnameVerifier(NoSsl.hostVerifier)
  }

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
