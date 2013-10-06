package goat.module

import goat.core.Module
import goat.core.Message
import goat.core.Constants._
import goat.core.KVStore
import goat.util.CommandParser
import goat.util.Passwords
import goat.util.StringUtil.scrub
import goat.util.TextFilters.scotchify
import goat.util.TranslateWrapper

import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.egit.github.core.service.CommitService
import org.eclipse.egit.github.core.service.IssueService
import org.eclipse.egit.github.core.service.LabelService
import org.eclipse.egit.github.core.service.OAuthService
import org.eclipse.egit.github.core.RepositoryCommit
import org.eclipse.egit.github.core.CommitFile
import org.eclipse.egit.github.core.Issue
import org.eclipse.egit.github.core.Label
import org.eclipse.egit.github.core.RepositoryIssue
import org.eclipse.egit.github.core.User
import org.eclipse.egit.github.core.Authorization
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.client.PageIterator

import java.util.Date
import java.util.TimeZone
import java.util.concurrent.ConcurrentNavigableMap

import scala.collection.JavaConversions._
import scala.collection.mutable.Buffer
import scala.math.max

class Github extends Module {

  val translator = new TranslateWrapper();

  override def messageType = Module.WANT_COMMAND_MESSAGES

  def getCommands(): Array[String] =
    Array("commits", "issues", "commit", "issue", "goatbug", "goatbugs")

  def processPrivateMessage(m: Message) =
    processChannelMessage(m)

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
  private val labelService = new LabelService(githubClient)
  private val goatRepo = repositoryService.getRepository("bcorrigan", "goat")

  private val utc = TimeZone.getTimeZone("UTC")
  private val timeFormat = new java.text.SimpleDateFormat("H:mm")
  private val dateFormat = new java.text.SimpleDateFormat("d MMM")
  private def formatUtcTime(date: Date): String = {
    timeFormat.setTimeZone(utc)
    val now = new Date
    if(now.getTime - date.getTime > 24 * 60 * 60 * 1000)
      dateFormat.format(date)
    else
      timeFormat.format(date)
  }

  private val separator = DARK_BLUE + BOLD + " \u00A7  " + NORMAL

  private def commitsReport: String = {
    updateCommitStore
    (0 to math.min(16, commitStore.size)).map((i: Int) => commitStore(commitStore.size - i - 1)).map(shortCommit(_)).reduce(_ + separator + _) +
    ".  " + separator + separator + " for more, see " + goatRepo.getHtmlUrl + "/issues"
  }

  private def commit(num: Int): String = {
    updateCommitStore
    if(num > commitStore.size)
      "I only have " + commitStore.size + " commits."
    else if(num == 0)
      "Nerd."
    else if(num > 0)
      longCommit(getCommit(num - 1))
    else if(-num > commitStore.size)  // num is negative here
      "I'm only up to " + commitStore.size + " commits."
    else
      longCommit(getCommit(commitStore.size + num)) // num is negative here
  }

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
        val closedPager = issueService.pageIssues(goatRepo, Map[String, String](("state", "closed")))
        val maxNum = {
          if (closedPager.hasNext) {
            val closedPage = closedPager.next
            if (closedPage.isEmpty)
              page.head.getNumber;
            else
              max(closedPage.head.getNumber, page.head.getNumber)
          }
          else
            page.head.getNumber;
        }
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

  private def emojiForIssue(issue: Issue): String =
    if (issue.getLabels.isEmpty) ""
    else issue.getLabels.head.getName match {
      case "Shite"  => OLIVE + PILE_OF_POO + "  " + NORMAL
      case "Demand" => DARK_GREEN + CHRISTMAS_TREE + "  " + NORMAL
      case "Whine"  => RED + BROKEN_HEART + "  " + NORMAL
      case _ => ""
    }

  private def shortIssue (issue: Issue): String =
    "#" + issue.getNumber + " " +
    formatUtcTime(issue.getCreatedAt) + " " +
    "(" + getComplainer(issue) + assignedString(issue) + ") " +
    emojiForIssue(issue) +
    titleString(issue)

  private def longIssue(ri: Issue): String =
    "Issue #" + ri.getNumber + ":  " +
    emojiForIssue(ri) +
    titleString(ri) + "  " +
    RED + "Complainer: " + getComplainer(ri) + NORMAL + " " +
    {if (ri.getAssignee != null) DARK_BLUE + "Fixer: " + ri.getAssignee.getLogin + NORMAL + " " else ""} +
    {if (ri.getComments > 0) PURPLE + "Comments: " + ri.getComments + NORMAL + " " else ""} +
    {if (ri.getState.equals("closed")) BOLD + "Closed: " + NORMAL + ri.getClosedAt
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

  private def crapSender(m: Message): Boolean =
    m.getSender.equals("ubu") || m.getHostmask.contains("austin.res.rr.com")

  private def buildIssue(m: Message): Issue = {
    val issue = new Issue
    val cp = new CommandParser(m)
    if (crapSender(m)) {
      val labels: Buffer[Label] = labelService.getLabels(goatRepo)
      issue.setLabels(List[Label](new Label().setName("Shite")))
    } else if (cp.hasVar("label")) {
      val labels: Buffer[Label] = labelService.getLabels(goatRepo)
      val parsedLabel = scrub(cp.get("label"))
      labels.find( l => l.getName.toLowerCase.equals(parsedLabel.toLowerCase)) match {
        case Some(found: Label) => issue.setLabels(List[Label](found))
        case None => {
          m.reply("Label \"" + parsedLabel + "\" is not a valid, and was ignored.  " + {
            if (labels.isEmpty) "There are no valid labels at present."
            else "Valid labels:  " + labels.map(l => l.getName).reduce(_ + " | " + _) })
        }
      }
    }
    val (title: String, body: String) =
      if(cp.hasVar("title") && ! scrub(cp.get("title")).equals(""))
        (scrub(cp.get("title")), scrub(cp.remaining))
      else if (cp.remaining.length < 80)
        (cp.remaining, "")
      else {
        val splitpoint = cp.remaining.substring(0, 72).lastIndexOf(" ")
        (cp.remaining.substring(0, splitpoint).trim + "\u20206", "... " + cp.remaining.substring(splitpoint).trim)
      }
    issue.setTitle("\u00AB" + m.getSender + "\u00BB " + translator.localize(m, title))
    if (! body.equals(""))
      issue.setBody(translator.localize(m, body))
    issue
  }

  // this doesn't work, don't know why
  private def closeIssue(issue: Issue): Unit = {
    issue.setState("closed")
    issueService.editIssue(goatRepo, issue)
  }

  private val goatbugUsage = "You're supposed to say: " +
                             DARK_BLUE + "goatbug [title=\"my complaint\"] " +
                             "[label=" + labelService.getLabels(goatRepo).map(_.getName).reduce(_ + "|" + _) + "] " +
                             "moan whinge bellyache  " + NORMAL +
                             "Or to view an existing bug: " +
                             DARK_BLUE + "goatbug [number]  " + NORMAL +
                             "Or to list all the bugs I know about: " +
                             DARK_BLUE + "goatbugs  " + NORMAL

  private def goatbug(m: Message) = {
    val cp = new CommandParser(m)
    if (cp.hasVar("title")) {
      if(scrub(cp.get("title")).equals(""))
        m.reply("I'll need a title with a little more substance")
      else
        m.reply(confirmIssue(issueService.createIssue(goatRepo, buildIssue(m))))
    }
    else if (scrub(cp.remaining).equals(""))
      m.reply(goatbugUsage)
    else if (cp.hasOnlyNumber)
      showIssue(m)
    else if (scrub(cp.remaining).toLowerCase.equals("help"))
      m.reply(goatbugUsage)
    else if (scrub(cp.remaining).length < 24) // ttly arbitrary
      m.reply("Try to be a bit more descriptive?")
    else if (crapSender(m)) {
      val issue = issueService.createIssue(goatRepo, buildIssue(m))
      m.reply(confirmIssue(issue))
      //closeIssue(issue)
    }
    else
      m.reply(confirmIssue(issueService.createIssue(goatRepo, buildIssue(m))))
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

  var lastCommitUpdate: Date = new Date(0)
  val cacheTimeout = 1 * 60 * 1000
  val commitStore: ConcurrentNavigableMap[Integer, RepositoryCommit] = KVStore.getDB().getTreeMap("githubCommits")

  private def getCommit(num: Int): RepositoryCommit =
    commitService.getCommit(goatRepo, commitStore.get(commitStore.size - num - 1).getSha)

  private def getSha(num: Int): String = commitStore.get(commitStore.size - num - 1).getSha

  private def updateCommitStore = {
    if(lastCommitUpdate.getTime + cacheTimeout < (new Date).getTime) {
      val (commits, startKey) =
        if (commitStore.isEmpty)
          (commitService.getCommits(goatRepo).toList, 0)
        else
          (getNewCommits, commitStore.size)
      for((commit, i) <- commits.reverse.view.zipWithIndex)
        commitStore.put(startKey + i, commit)
      KVStore.getDB.commit
      lastCommitUpdate = new Date
    }
  }

  private def getNewCommits: List[RepositoryCommit] = {
    val oldHeadSha = commitStore.get(commitStore.lastKey).getSha
    val newCommitsPager = commitService.pageCommits(goatRepo, 32)  // 32 is arbitrary
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

}
