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

import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

import net.liftweb.json._

import scala.io.Source
import scala.sys.process.Process

object Main extends App {
  def extractChildIssues(epic: String): List[String] = {
    HttpsURLConnection.setDefaultSSLSocketFactory(NoSsl.socketFactory)
    HttpsURLConnection.setDefaultHostnameVerifier(NoSsl.hostVerifier);

    val connection = new URL(Settings.jiraUrl.format(epic)).openConnection
    connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION, HttpBasicAuth.getHeader(Settings.jiraUser, Settings.jiraPassword))
    val jsonResult = parse(Source.fromInputStream(connection.getInputStream).mkString)
    val issues = jsonResult \ "issues" \ "key"
    issues.values.asInstanceOf[List[(String, String)]].map(tuple => tuple._2)
  }

  def removeCommitLines(result: String): String = {
    result.split("\n").filter(line => !line.matches("^.{7} .*")).mkString("\n")
  }

  private val listChangedFiles = Seq("git", "log", "--no-merges", "--name-only", "--oneline") ++
    Settings.epics.flatMap(epic => extractChildIssues(epic)).map(issue => s"--grep=$issue ")

  println(removeCommitLines(Process(listChangedFiles, new File(Settings.gitRepository)).!!))
}
