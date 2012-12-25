package goat.module

import goat.core.Module
import goat.core.Message
import goat.core.Constants._
import org.eclipse.egit.github.core.service.RepositoryService
import org.eclipse.egit.github.core.service.CommitService
import org.eclipse.egit.github.core.RepositoryCommit
import org.eclipse.egit.github.core.client.PageIterator

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
        m.reply("Goatgit!")
      case "commits" =>
        m.reply(commitReport)
      case "issues" =>
        m.reply(issueReport)
    }
  }

  val repositoryService = new RepositoryService()
  val commitService = new CommitService()
  val goatRepo = repositoryService.getRepository("bcorrigan", "goat")

  def commitReport: String =
    commitReport_1("", commitService.pageCommits(goatRepo, 10).next().iterator())

  def commitReport_1(report: String, iter: Iterator[RepositoryCommit]): String =
    if (iter.hasNext())
      if (report == "")
        commitReport_1(formatCommit(iter.next()), iter)
      else
        commitReport_1(report + "  " + GOATJI + "  " + formatCommit(iter.next()), iter)
    else
      report

  def formatCommit(rc: RepositoryCommit): String =
    rc.getCommit().getCommitter().getDate() + ", " +
    rc.getCommit.getCommitter().getName() + " - " +
    rc.getCommit().getMessage()

  def issueReport = "goat!"

  def issues = "undefined"
}
