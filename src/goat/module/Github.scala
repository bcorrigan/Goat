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
    Array("commits", "issues", "commit", "issue", "goatbug", "goatbugs")
  }

  def processPrivateMessage(m: Message) = {
    processChannelMessage(m)
  }

  def processChannelMessage(m: Message) =
    try {
      m.getModCommand.toLowerCase match {
        case "commits" =>
          m.reply(commitsReport)
        case "issues" | "goatbugs" =>
          m.reply(issuesReport)
        case "commit" =>
          m.reply(commit(new CommandParser(m).findNumber.toInt))
        case "issue" =>
          showIssue(m)
        case "goatbug" =>
          goatbug(m)
      }
    } catch {
      case nfe: NumberFormatException =>
        m.reply("I don't believe that's a number.")
    }
  
  private val githubClient = getAuthorizedClient
  
  private val repositoryService = new RepositoryService(githubClient)
  private val commitService = new CommitService(githubClient)
  private val issueService = new IssueService(githubClient)
  private val goatRepo = repositoryService.getRepository("bcorrigan", "goat")
  
  private val utc = TimeZone.getTimeZone("UTC")
  private val timeFormat = new java.text.SimpleDateFormat("d MMM H:mm")
  private def formatUtcTime(date: Date): String = {
	 timeFormat.setTimeZone(utc) // there's no SimpleDateFormat constructor that takes a TimeZone, boo
	 timeFormat.format(date)
  }
  
  private val separator = DARK_BLUE + BOLD + " \u00A7  " + NORMAL
  
  private def commitsReport: String =
    commits.slice(0, 9).map(shortCommit(_)).reduce(_ + separator + _)
    
  private def commit(num: Int): String =
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

  private def shortCommit(rc: RepositoryCommit): String =
    formatUtcTime(rc.getCommit.getCommitter.getDate) + " " +
    "(" + rc.getCommit.getCommitter.getName + ") " +
    rc.getCommit.getMessage
  
  private def longCommit(rc: RepositoryCommit): String =
    rc.getCommit.getMessage + "  " +
  	"(" + rc.getCommit.getCommitter.getName + ") " +
    {if (rc.getStats.getAdditions > 0) DARK_BLUE + "+" + rc.getStats.getAdditions + " " + NORMAL else ""} +
    {if (rc.getStats.getDeletions > 0) RED + "-" + rc.getStats.getDeletions + " " + NORMAL else "" } +
    {if (rc.getCommit.getCommentCount > 0) OLIVE + rc.getCommit.getCommentCount + " Comments " + NORMAL else ""} +
    rc.getCommit.getCommitter.getDate + " " +
    "https://github.com/bcorrigan/Goat/commit/" + rc.getSha() + "  " +
    filesReport(rc)

  private def filesReport(rc: RepositoryCommit): String =
    if(rc.getFiles == null || rc.getFiles.isEmpty)
      ""
    else
      "Files:  " + rc.getFiles.map(fileSummary(_)).reduce(_ + ", " + _)
      
  private def fileSummary(cf: CommitFile): String = 
    cf.getFilename + " " +
    "(" + DARK_BLUE + "+" + cf.getAdditions + " " +
    RED + "-" + cf.getDeletions + NORMAL + ")"
  
  private def issuesReport: String = {
    val issues = issueService.getIssues(goatRepo, null)
    if(issues.isEmpty())
      "No issues."
    else
      issues.map(shortIssue(_)).reduce(_ + separator + _)
  }

  private def showIssue(m:Message):Unit = {
    val cp = new CommandParser(m)
    if (cp.hasNumber)
      m.reply(issue(cp.findNumber.toInt))
    else
      m.reply("I need an issue number.  You can list all issues with \"issues\"")
  }
    
  private def issue(num: Int): String = { 
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

  private def shortIssue (issue: Issue): String =
    "#" + issue.getNumber + " " +
    formatUtcTime(issue.getCreatedAt) + " " +
    "(" + getComplainer(issue) + assignedString(issue) + ") " +
    titleString(issue)

  private def longIssue(ri: Issue): String = 
    "Issue #" + ri.getNumber + ":  " +
    titleString(ri) + "  " +
    RED + "Complainer: " + getComplainer(ri) + NORMAL + " " +
    {if (ri.getAssignee != null) DARK_BLUE + "Fixer: " + ri.getAssignee.getLogin + NORMAL + " " else ""} +
    {if (ri.getComments > 0) OLIVE + "Comments: " + ri.getComments + NORMAL + " " else ""} +
    {if (ri.getState.equals("closed")) BOLD + "Closed: " + NORMAL + formatUtcTime(ri.getClosedAt) 
      else ri.getCreatedAt} + " " +
    ri.getHtmlUrl + "  " +
    {if (ri.getBody == null) "" else ri.getBody}

  private def titleString(issue: Issue): String = 
    if(issue.getUser.getLogin.equals("jgoat") && hasIrcUser(issue))
      issue.getTitle.substring(issue.getTitle.indexOf("\u00BB") + 1).trim
    else
      issue.getTitle
    
  private def assignedString(issue: Issue): String =
    if(issue.getAssignee == null)
      ""
    else
      " -> " + issue.getAssignee.getLogin    
        
  private def getComplainer(issue: Issue): String =
    if(issue.getUser.getLogin.equals("jgoat"))
      if(hasIrcUser(issue))
        issue.getTitle.substring(1, issue.getTitle.indexOf("\u00BB"))
      else
        "goat"
    else
      issue.getUser.getLogin
      
  private def hasIrcUser(issue: Issue): Boolean =
    issue.getTitle.substring(0,1).equals("\u00AB") && issue.getTitle.substring(1).contains("\u00BB")
      
  private def buildIssue(m: Message): Issue = {
      val issue = new Issue
      val cp = new CommandParser(m)
      val (title: String, body: String) = 
        if(cp.hasVar("title") && ! removeFormattingAndColors(cp.get("title")).trim.equals(""))
          (removeFormattingAndColors(cp.get("title")).trim, removeFormattingAndColors(cp.remaining).trim)
        else if (cp.remaining.length < 80)
          (cp.remaining, "")
        else {
          val splitpoint = cp.remaining.substring(0, 72).lastIndexOf(" ")
          (cp.remaining.substring(0, splitpoint).trim + "\u20206", "... " + cp.remaining.substring(splitpoint).trim)
        } 
      issue.setTitle("\u00AB" + m.getSender + "\u00BB " + title)
      if (! body.equals(""))
        issue.setBody(body)
      issue
   }
   
  private val goatbugUsage = "You're supposed to say: " +
                             DARK_BLUE + "goatbug [title=\"my complaint\"] moan whinge bellyache  " + NORMAL +
                             "Or to view an existing bug: " +
                             DARK_BLUE + "goatbug [number]  " + NORMAL +
                             "Or to list all the bugs I know about: " +
                             DARK_BLUE + "goatbugs  " + NORMAL
  
  private def goatbug(m: Message) = {
     val cp = new CommandParser(m)
     if (removeFormattingAndColors(cp.remaining).equals(""))
       m.reply(goatbugUsage)
     else if (cp.hasOnlyNumber)
       showIssue(m)
     else
       confirmIssue(issueService.createIssue(goatRepo, buildIssue(m)))
  }
       
   def confirmIssue(issue: Issue): String = 
     BOLD + "Bug reported!  " + NORMAL +
     shortIssue(issue) + "  " +
     issue.getHtmlUrl
        
  // OAuth gunk
  private def getAuthorizedClient(): GitHubClient = {
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
  
  private def getCommit(num: Int): RepositoryCommit = 
    commitService.getCommit(goatRepo, commits(num).getSha)
  
  private def getSha(num: Int): String = commitsListBuffer(num).getSha
  
  private def commits: Buffer[RepositoryCommit] =
    if (commitsListBuffer == null || commitsListBuffer.isEmpty) 
      initCommitsBuffer
    else if((new Date).getTime - lastCommitsBufferUpdate.getTime > cacheTimeout) 
      updateCommitsBuffer
    else commitsListBuffer
    
  private def initCommitsBuffer: Buffer[RepositoryCommit] = {
    if (commitsListStore.has("commits")) 
      commitsListBuffer = commitsListStore.get("commits")
    updateCommitsBuffer
  }
  
  private def updateCommitsBuffer: Buffer[RepositoryCommit] = {
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
  
  private def getNewCommits: List[RepositoryCommit] = {
    val oldHeadSha = commitsListBuffer.head.getSha
    val newCommitsPager = commitService.pageCommits(goatRepo, 32)  // 32 is more or less arbitrary
    getCommitsUntilSha(newCommitsPager.next, newCommitsPager, oldHeadSha, List[RepositoryCommit]()).dropRight(1)
  }
  
  // it's mildly offensive that we need to implement this ourselves
  private def getCommitsUntilSha(page: java.util.Collection[RepositoryCommit], 
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
