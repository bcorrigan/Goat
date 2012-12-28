package goat.module

import goat.core.Module
import goat.core.Message
import goat.core.Constants._

import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.egit.github.core.service.CommitService
import org.eclipse.egit.github.core.RepositoryCommit
import org.eclipse.egit.github.core.service.IssueService
import org.eclipse.egit.github.core.Issue
import org.eclipse.egit.github.core.client.PageIterator

import java.util.Date
import java.util.TimeZone

import scala.collection.JavaConversions._

class Github extends Module {

  override def messageType = Module.WANT_COMMAND_MESSAGES

  def getCommands(): Array[String] = {
    Array("git", "commits", "issues")
  }

  def processPrivateMessage(m: Message) = {
    processChannelMessage(m)
  }

  def processChannelMessage(m: Message) = {
    m.getModCommand().toLowerCase() match {
      case "git" =>
        m.reply("I know \"commits\" and \"issues\"")
      case "commits" =>
        m.reply(commitReport)
      case "issues" =>
        m.reply(issueReport)
    }
  }

  val repositoryService = new RepositoryService()
  val commitService = new CommitService()
  val issueService = new IssueService()
  val goatRepo = repositoryService.getRepository("bcorrigan", "goat")
  
  val utc = TimeZone.getTimeZone("UTC")
  val timeFormat = new java.text.SimpleDateFormat("d MMM H:mm")
  def formatUtcTime(date: Date): String = {
	 timeFormat.setTimeZone(utc) // there's no SimpleDateFormat constructor that takes a TimeZone, boo
	 timeFormat.format(date)
  }
  
  val separator = DARK_BLUE + " \u00A7  " + NORMAL
  
  def commitReport: String =
    commitService.pageCommits(goatRepo, 10).next().map(formatCommit(_)).reduce(_ + separator + _)
    
  def formatCommit(rc: RepositoryCommit): String =
    formatUtcTime(rc.getCommit().getCommitter().getDate()) + " " +
    "(" + rc.getCommit.getCommitter().getName() + ") " +
    rc.getCommit().getMessage()

  def issueReport: String =
    issueService.pageIssues(goatRepo).next().map(formatIssue(_)).reduce(_ + separator + _)
  
  def formatIssue (issue: Issue): String =
    formatUtcTime(issue.getCreatedAt()) + " " +
    "(" + issue.getUser().getLogin() + ") " +
    issue.getTitle()
    
}
