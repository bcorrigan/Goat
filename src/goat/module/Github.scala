package goat.module

import goat.core.Module
import goat.core.Message
import goat.core.Constants._
import goat.core.KVStore
import goat.util.CommandParser
import goat.util.Passwords
import goat.util.StringUtil.removeFormattingAndColors

import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.egit.github.core.service.CommitService
import org.eclipse.egit.github.core.service.IssueService
import org.eclipse.egit.github.core.RepositoryCommit
import org.eclipse.egit.github.core.CommitFile
import org.eclipse.egit.github.core.Issue
import org.eclipse.egit.github.core.RepositoryIssue
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.client.PageIterator

import java.util.Date
import java.util.TimeZone

import scala.collection.JavaConversions._
import scala.collection.mutable.Buffer
import scala.Math.max

class Github extends Module {

  override def messageType = Module.WANT_COMMAND_MESSAGES

  def getCommands(): Array[String] = {
    Array("git", "commits", "issues", "commit", "issue")
  }

  def processPrivateMessage(m: Message) = {
    processChannelMessage(m)
  }

  def processChannelMessage(m: Message) =
    try {
      m.getModCommand.toLowerCase match {
        case "git" =>
          m.reply("I know \"commits\" and \"issues\"")
        case "commits" =>
          m.reply(commitsReport)
        case "issues" =>
          m.reply(issuesReport)
        case "commit" =>
          m.reply(commit(findNum(m)))
        case "issue" =>
          m.reply(issue(findNum(m)))
      }
    } catch {
      case nfe: NumberFormatException =>
        m.reply("I don't believe that's a number.")
    }

  def findNum(m: Message): Int = {
    val cp = new CommandParser(m)
    if (cp.hasVar("num"))
      removeFormattingAndColors(cp.get("num")).toInt
    else
      removeFormattingAndColors(cp.remaining).toInt
  }
  
  val githubClient = new GitHubClient
  githubClient.setCredentials(Passwords.getPassword("github.login"), Passwords.getPassword("github.password"))

  val repositoryService = new RepositoryService(githubClient)
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
    commits.slice(0, 9).map(shortCommit(_)).reduce(_ + separator + _)
    
  def commit(num: Int): String =
    if(num > commits.size)
      "I only have " + commits.size + " commits."
    else if(num == 0)
      "Nerd."
    else if(num > 0)
      longCommit(getCommit(num - 1))
    else if(-num > commits.size)  // num is negative here
      "I'm only up to " + commits.size + " commits."
    else
      longCommit(getCommit(commits.size + num)) // num is negative here

  def shortCommit(rc: RepositoryCommit): String =
    formatUtcTime(rc.getCommit.getCommitter.getDate) + " " +
    "(" + rc.getCommit.getCommitter.getName + ") " +
    rc.getCommit.getMessage
  
  def longCommit(rc: RepositoryCommit): String =
    rc.getCommit.getMessage + "  " +
  	"(" + rc.getCommit.getCommitter.getName + ") " +
    {if (rc.getStats.getAdditions > 0) DARK_BLUE + "+" + rc.getStats.getAdditions + " " + NORMAL else ""} +
    {if (rc.getStats.getDeletions > 0) RED + "-" + rc.getStats.getDeletions + " " + NORMAL else "" } +
    {if (rc.getCommit.getCommentCount > 0) OLIVE + rc.getCommit.getCommentCount + " Comments " + NORMAL else ""} +
    rc.getCommit.getCommitter.getDate + " " +
    "https://github.com/bcorrigan/Goat/commit/" + rc.getSha() + "  " +
    filesReport(rc)

  def filesReport(rc: RepositoryCommit): String =
    if(rc.getFiles == null || rc.getFiles.isEmpty)
      ""
    else
      "Files:  " + rc.getFiles.map(fileSummary(_)).reduce(_ + ", " + _)
      
  def fileSummary(cf: CommitFile): String = 
    cf.getFilename + " " +
    "(" + DARK_BLUE + "+" + cf.getAdditions + " " +
    RED + "-" + cf.getDeletions + NORMAL + ")"
  
  def issuesReport: String = {
    val issuepage = issueService.pageIssues(goatRepo)
    if(issuepage.hasNext)
      issuepage.next.map(shortIssue(_)).reduce(_ + separator + _)
    else
      "No issues."
  }

  def issue(num: Int): String = { 
    val openIssues: Int = goatRepo.getOpenIssues
    if(num < 1)
      "You're such a kidder."
    else if(num > openIssues)
      "My issues only go up to #" + openIssues
    else
      longIssue(issueService.getIssue(goatRepo, num))
  }

  def shortIssue (issue: Issue): String =
    "#" + issue.getNumber + " " +
    formatUtcTime(issue.getCreatedAt) + " " +
    "(" + issue.getUser.getLogin + assignedString(issue) + ") " +
    issue.getTitle

  def longIssue(ri: Issue): String = 
    "Issue #" + ri.getNumber + ":  " +
    ri.getTitle + "  " +
    RED + "Complainer: " + ri.getUser.getLogin + NORMAL + " " +
    {if (ri.getAssignee != null) DARK_BLUE + "Fixer: " + ri.getAssignee.getLogin + NORMAL + " " else ""} +
    {if (ri.getComments > 0) OLIVE + "Comments: " + ri.getComments + NORMAL + " " else ""} +
    ri.getHtmlUrl + "  " +
    {if (ri.getBody == null) "" else ri.getBody}

  def assignedString(issue: Issue): String =
    if(issue.getAssignee == null)
      ""
    else
      " -> " + issue.getAssignee.getLogin    
        
  // Getting the nth most recent commit via github's api is, um, inconvenient.
  // So we do some caching.  Caching is no fun.
    
  var commitsListBuffer: Buffer[RepositoryCommit] = null
  var lastCommitsBufferUpdate: Date = new Date(0)
  val cacheTimeout = 1 * 60 * 1000
  val commitsListStore = new KVStore[Buffer[RepositoryCommit]]("github.commitList")
  val commitStore = new KVStore[RepositoryCommit]("github.commits")
  
  def getCommit(num: Int): RepositoryCommit = 
    commitService.getCommit(goatRepo, commits(num).getSha)
  
  def getSha(num: Int): String = commitsListBuffer(num).getSha
  
  def commits: Buffer[RepositoryCommit] =
    if (commitsListBuffer == null || commitsListBuffer.isEmpty) 
      initCommitsBuffer
    else if((new Date).getTime - lastCommitsBufferUpdate.getTime > cacheTimeout) 
      updateCommitsBuffer
    else commitsListBuffer
    
  def initCommitsBuffer: Buffer[RepositoryCommit] = {
    if (commitsListStore.has("commits")) 
      commitsListBuffer = commitsListStore.get("commits")
    updateCommitsBuffer
  }
  
  def updateCommitsBuffer: Buffer[RepositoryCommit] = {
    if (commitsListBuffer == null || commitsListBuffer.isEmpty) {
      commitsListBuffer = commitService.getCommits(goatRepo)
      cacheCommits
    } else {
      val newCommits = getNewCommits
      if(newCommits.isEmpty)
        commitsListBuffer
      else {
        commitsListBuffer.prependAll(newCommits)
        cacheCommits
      }
    }
  }
  
  def getNewCommits: List[RepositoryCommit] = {
    val oldHeadSha = commitsListBuffer.head.getSha
    val newCommitsPager = commitService.pageCommits(goatRepo, 32)  // 32 is more or less arbitrary
    getCommitsUntilSha(newCommitsPager.next, newCommitsPager, oldHeadSha, List[RepositoryCommit]()).dropRight(1)
  }
  
  // it's mildly offensive that we need to implement this ourselves
  def getCommitsUntilSha(page: java.util.Collection[RepositoryCommit], 
		  		         pager: PageIterator[RepositoryCommit], 
		  		         sha: String, 
		  		         newCommits: List[RepositoryCommit]): List[RepositoryCommit] = 
    if (page.isEmpty)
      if (pager.hasNext)
        getCommitsUntilSha(pager.next, pager, sha, newCommits)
      else
        List[RepositoryCommit]() // should probably throw an exception here; we've reached the end without finding sha
    else if (page.head.getSha.equals(sha))
      (page.head :: newCommits).reverse
    else
      getCommitsUntilSha(page.tail, pager, sha, page.head :: newCommits)
      
  def cacheCommits: Buffer[RepositoryCommit] = {
    commitsListStore.save("commits", commitsListBuffer)
    lastCommitsBufferUpdate = new Date
    commitsListBuffer
  }
}
