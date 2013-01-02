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
import org.eclipse.egit.github.core.service.OAuthService
import org.eclipse.egit.github.core.RepositoryCommit
import org.eclipse.egit.github.core.CommitFile
import org.eclipse.egit.github.core.Issue
import org.eclipse.egit.github.core.RepositoryIssue
import org.eclipse.egit.github.core.User
import org.eclipse.egit.github.core.Authorization
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
    Array("git", "commits", "issues", "commit", "issue", "goatbug")
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
        case "goatbug" =>
          m.reply(confirmIssue(issueService.createIssue(goatRepo, buildIssue(m))))
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
  
  val githubClient = getAuthorizedClient
  
  val repositoryService = new RepositoryService(githubClient)
  val commitService = new CommitService(githubClient)
  val issueService = new IssueService(githubClient)
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
    val pager = issueService.pageIssues(goatRepo)
    if(pager.hasNext) {
      val page = pager.next
      if (page.isEmpty)
        "No Issues."
      else {
        val maxNum = page.head.getNumber()
        if(num < 1)
          "You're such a kidder."
        else if(num > maxNum)
          "My issues only go up to #" + maxNum
        else
          longIssue(issueService.getIssue(goatRepo, num))
      }
    } else
      "No Issues."
  }

  def shortIssue (issue: Issue): String =
    "#" + issue.getNumber + " " +
    formatUtcTime(issue.getCreatedAt) + " " +
    "(" + getComplainer(issue) + assignedString(issue) + ") " +
    titleString(issue)

  def longIssue(ri: Issue): String = 
    "Issue #" + ri.getNumber + ":  " +
    titleString(ri) + "  " +
    RED + "Complainer: " + getComplainer(ri) + NORMAL + " " +
    {if (ri.getAssignee != null) DARK_BLUE + "Fixer: " + ri.getAssignee.getLogin + NORMAL + " " else ""} +
    {if (ri.getComments > 0) OLIVE + "Comments: " + ri.getComments + NORMAL + " " else ""} +
    {if (ri.getState.equals("closed")) BOLD + "Closed: " + NORMAL + formatUtcTime(ri.getClosedAt) 
      else ri.getCreatedAt} + " " +
    ri.getHtmlUrl + "  " +
    {if (ri.getBody == null) "" else ri.getBody}

  def titleString(issue: Issue): String = 
    if(issue.getUser.getLogin.equals("jgoat") && hasIrcUser(issue))
      issue.getTitle.substring(issue.getTitle.indexOf("\u00BB") + 1).trim
    else
      issue.getTitle
    
  def assignedString(issue: Issue): String =
    if(issue.getAssignee == null)
      ""
    else
      " -> " + issue.getAssignee.getLogin    
        
  def getComplainer(issue: Issue): String =
    if(issue.getUser.getLogin.equals("jgoat"))
      if(hasIrcUser(issue))
        issue.getTitle.substring(1, issue.getTitle.indexOf("\u00BB"))
      else
        "goat"
    else
      issue.getUser.getLogin
      
  def hasIrcUser(issue: Issue): Boolean =
    issue.getTitle.substring(0,1).equals("\u00AB") && issue.getTitle.substring(1).contains("\u00BB")
      
  def buildIssue(m: Message): Issue = {
      val issue = new Issue
      val cp = new CommandParser(m)
      val remaining = removeFormattingAndColors(m.getTrailing).trim
      val (title: String, body: String) = 
        if(cp.hasVar("title") && ! removeFormattingAndColors(cp.get("title")).trim.equals(""))
          (removeFormattingAndColors(cp.get("title")).trim, removeFormattingAndColors(cp.remaining).trim)
        else if (remaining.length < 80)
          (remaining, "")
        else {
          val splitpoint = remaining.substring(0, 72).lastIndexOf(" ")
          (remaining.substring(0, splitpoint).trim + "\u20206", "... " + remaining.substring(splitpoint).trim)
        } 
      issue.setTitle("\u00AB" + m.getSender + "\u00BB " + title)
      if (! body.equals(""))
        issue.setBody(body)
      issue
   }
   
   def confirmIssue(issue: Issue): String = 
     BOLD + "Bug reported!  " + NORMAL +
     shortIssue(issue) + "  " +
     issue.getHtmlUrl
        
  // OAuth gunk
  def getAuthorizedClient(): GitHubClient = {
    val client = new GitHubClient
    if(Passwords.getPassword("github.token") != null && !Passwords.getPassword("github.token").equals(""))
      client.setOAuth2Token(Passwords.getPassword("github.token"))
    else {
      // um, retrieve our oauth token using basic auth...
      val basicClient = new GitHubClient
      var passwords = Passwords.getPasswords;
      basicClient.setCredentials(Passwords.getPassword("github.login"), Passwords.getPassword("github.password"))
      val authService = new OAuthService(basicClient)
      if(authService.getAuthorizations().isEmpty) {
        // create a new auth token if we don't have any
        val auth = new Authorization
        auth.setNote("goat!")
        auth.setScopes(List("user", "public_repo", "repo"))
        authService.createAuthorization(auth)
      }
      // just grab the first available auth token; might want to be smarter about this some day
      val token = authService.getAuthorizations.get(0).getToken
      passwords.put("github.token", token)
      Passwords.writePasswords(passwords)
      passwords = null
      client.setOAuth2Token(token)
    }
  }
      
  // Getting the nth most recent commit via github's api is, um, inconvenient.
  // So we do some caching.  Caching is no fun.
    
  var commitsListBuffer: Buffer[RepositoryCommit] = null
  var lastCommitsBufferUpdate: Date = new Date(0)
  val cacheTimeout = 1 * 60 * 1000
  val commitsListStore = new KVStore[Buffer[RepositoryCommit]]("github.commitList")
  
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
