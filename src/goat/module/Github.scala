package goat.module

import goat.core.Module
import goat.core.Message
import goat.core.Constants._
import goat.util.CommandParser

import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.egit.github.core.service.CommitService
import org.eclipse.egit.github.core.service.IssueService
import org.eclipse.egit.github.core.RepositoryCommit
import org.eclipse.egit.github.core.CommitFile
import org.eclipse.egit.github.core.Issue
import org.eclipse.egit.github.core.client.PageIterator

import java.util.Date
import java.util.TimeZone

import scala.collection.JavaConversions._

class Github extends Module {

  override def messageType = Module.WANT_COMMAND_MESSAGES

  def getCommands(): Array[String] = {
    Array("git", "commits", "issues", "commit")
  }

  def processPrivateMessage(m: Message) = {
    processChannelMessage(m)
  }

  def processChannelMessage(m: Message) = {
    m.getModCommand.toLowerCase match {
      case "git" =>
        m.reply("I know \"commits\" and \"issues\"")
      case "commits" =>
        m.reply(commitsReport)
      case "issues" =>
        m.reply(issuesReport)
      case "commit" => 
        m.reply(commit((new CommandParser(m)).get("num").toInt))
    }
  }

  val repositoryService = new RepositoryService
  val commitService = new CommitService
  val issueService = new IssueService
  val goatRepo = repositoryService.getRepository("bcorrigan", "goat")
  
  val utc = TimeZone.getTimeZone("UTC")
  val timeFormat = new java.text.SimpleDateFormat("d MMM H:mm")
  def formatUtcTime(date: Date): String = {
	 timeFormat.setTimeZone(utc) // there's no SimpleDateFormat constructor that takes a TimeZone, boo
	 timeFormat.format(date)
  }
  
  val separator = DARK_BLUE + BOLD + " \u00A7  " + NORMAL
  
  def commitsReport: String =
    commitService.pageCommits(goatRepo, 10).next.map(shortCommit(_)).reduce(_ + separator + _)
    
  def shortCommit(rc: RepositoryCommit): String =
    formatUtcTime(rc.getCommit.getCommitter.getDate) + " " +
    "(" + rc.getCommit.getCommitter.getName + ") " +
    rc.getCommit.getMessage

  def issuesReport: String =
    issueService.pageIssues(goatRepo).next.map(shortIssue(_)).reduce(_ + separator + _)
  
  def shortIssue (issue: Issue): String =
    formatUtcTime(issue.getCreatedAt) + " " +
    "(" + issue.getUser.getLogin + ") " +
    issue.getTitle
  
  // So, getting the nth most recent commit via github's api sucks donkey cock.
  //
  // this solution is grossly inefficient, but the blasted github api won't tell you 
  // how many commits are in a repository or branch, and pages are based on sha rather
  // than, oh, PAGE NUMBER, so we can't use the paged
  // methods to only get the page with the commit we want on it.
  // also, the list of all commits you can request is of summaries of commits, so we've got to
  // grab the sha and use that to go get the full data for the commit.  Uggo.

  // this needs to be reworked, most likely with some sort of persistence, and even
  //   then it will take >10hr to suck all of goat's commits through the 60 req/hr
  //   limit for unauthed api clients.
    
  // Pooooop.
    
  def commit(num: Int): String = longCommit(commitService.getCommit(goatRepo, getSha(num)))

  def getSha(num: Int): String = commitService.getCommits(goatRepo)(num).getSha
  
  def longCommit(rc: RepositoryCommit): String =
  	rc.getCommit.getCommitter.getDate + " " +
  	"(" + rc.getCommit.getCommitter.getName + ") " +
    DARK_BLUE + "+" + rc.getStats.getAdditions + " " +
    RED + "-" + rc.getStats.getDeletions + " " +
    NORMAL + rc.getCommit.getMessage + "  " +
    "Files:  " + rc.getFiles.map(fileSummary(_)).reduce(_ + ", " + _)
    
  def fileSummary(cf: CommitFile): String = 
    cf.getFilename + " " +
    "(" + DARK_BLUE + "+" + cf.getAdditions + " " +
    RED + "-" + cf.getDeletions + NORMAL + ")"
    
}
