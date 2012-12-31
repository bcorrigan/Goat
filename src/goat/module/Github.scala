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
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.client.PageIterator

import java.util.Date
import java.util.TimeZone

import scala.collection.JavaConversions._
import scala.collection.mutable.Buffer

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
        try {
          val cp = new CommandParser(m)
          if (cp.hasVar("num"))
          	m.reply(commit(removeFormattingAndColors(cp.get("num")).toInt))
          else if (cp.hasRemaining)
            m.reply(commit(removeFormattingAndColors(cp.remaining).toInt))
        } catch {
          case nfe: NumberFormatException =>
            m.reply("I don't believe that's a number.")
        }
    }
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
    
  def longCommit(rc: RepositoryCommit): String =
    rc.getSha + " " +
  	rc.getCommit.getCommitter.getDate + " " +
  	"(" + rc.getCommit.getCommitter.getName + ") " +
    DARK_BLUE + "+" + rc.getStats.getAdditions + " " +
    RED + "-" + rc.getStats.getDeletions + " " +
    NORMAL + rc.getCommit.getMessage + "  " +
    "Files:  " + filesReport(rc)

  def filesReport(rc: RepositoryCommit): String =
    if(rc.getFiles == null || rc.getFiles.isEmpty)
      "no files."
    else
      rc.getFiles.map(fileSummary(_)).reduce(_ + ", " + _)
      
  def fileSummary(cf: CommitFile): String = 
    cf.getFilename + " " +
    "(" + DARK_BLUE + "+" + cf.getAdditions + " " +
    RED + "-" + cf.getDeletions + NORMAL + ")"
    
  // Getting the nth most recent commit via github's api is, um, inconvenient.
  // So we do some caching.  Which adds up to more than half of our code.
    
  var commitsListBuffer: Buffer[RepositoryCommit] = null
  var lastCommitsBufferUpdate: Date = new Date(0)
  val cacheTimeout = 1 * 60 * 1000
  val commitsListStore = new KVStore[Buffer[RepositoryCommit]]("github.commitList")
  val commitStore = new KVStore[RepositoryCommit]("github.commits")
  
  def getCommit(num: Int): RepositoryCommit = {
    if(commitsListBuffer == null)
      initCommitsBuffer
    val sha = commitsListBuffer(num).getSha
    if(commitStore.has(sha))
      commitStore.get(sha)
    else
      fetchCommit(sha)
  }
    
  def fetchCommit(sha: String): RepositoryCommit = {
    val commit = commitService.getCommit(goatRepo, sha)
    commitStore.save(sha, commit)
    commit
  }
  
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
    val newCommitsPager = commitService.pageCommits(goatRepo, 32)
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
