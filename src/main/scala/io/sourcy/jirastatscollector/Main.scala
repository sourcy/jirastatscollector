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
  HttpsURLConnection.setDefaultSSLSocketFactory(NoSsl.socketFactory)
  HttpsURLConnection.setDefaultHostnameVerifier(NoSsl.hostVerifier);

  private val allIssues = extractAllIssues()
  private val gitLogOutput: String = Process(listChangedFiles(allIssues), new File(Settings.gitRepository)).!!
  private val changedFiles = filterLogOutput(gitLogOutput, line => !line.matches("^.{7} .*")).distinct
  private val commits = filterLogOutput(gitLogOutput, line => line.matches("^.{7} .*"))
  private val gitDiffOutput: String = Process(listChangedLines(allIssues), new File(Settings.gitRepository)).!!
  private val changedLines = filterLogOutput(gitDiffOutput, line => line.matches("^[\\+\\-] .*"))

  println(s"${changedFiles.size} files changed.")
  println(s"${changedLines.size} lines changed.")
  println(s"${commits.size} commits pushed.")
  println(s"${allIssues.size} issues.")

  private def extractChildIssues(epic: String): List[String] = {
    val connection = new URL(Settings.jiraUrl + "jql=" + Settings.epicCustomField + "=%s".format(epic)).openConnection
    connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION, HttpBasicAuth.getHeader(Settings.jiraUser, Settings.jiraPassword))
    val jsonResult = parse(Source.fromInputStream(connection.getInputStream).mkString)
    val issuesNode = jsonResult \ "issues" \ "key"
    issuesNode.values.asInstanceOf[List[(String, String)]].map(tuple => tuple._2)
  }

  private def extractSubTasks(issue: String): List[String] = {
    val connection = new URL(Settings.jiraUrl + "jql=" + "parent=%s".format(issue)).openConnection
    connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION, HttpBasicAuth.getHeader(Settings.jiraUser, Settings.jiraPassword))
    val jsonResult = parse(Source.fromInputStream(connection.getInputStream).mkString)
    val issuesNode = jsonResult \ "issues" \ "key"
    val values = issuesNode.values
    var ret: List[String] = List()
    try {
      ret = values.asInstanceOf[List[(String, String)]].map(tuple => tuple._2)
    } catch {
      case e: ClassCastException =>
    }
    issue :: ret
  }

  def extractAllIssues(): Seq[String] = {
    Settings.epics.flatMap(epic => extractChildIssues(epic).flatMap(issue => extractSubTasks(issue)))
  }

  private def filterLogOutput(result: String, filter: (String) => Boolean): List[String] = {
    result.split("\n").filter(filter).toList
  }

  private def listChangedFiles(issues: Seq[String]) =
    Seq("git", "log", "--no-merges", "--name-only", "--oneline") ++ issues.map(issue => s"--grep=$issue")

  private def listChangedLines(issues: Seq[String]) =
    Seq("git", "log", "--no-merges", "--full-diff", "-p", "--no-renames") ++ issues.map(issue => s"--grep=$issue")

}
