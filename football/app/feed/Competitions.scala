package feed

import common.{ Logging, AkkaSupport }
import akka.actor.Cancellable
import org.joda.time.{ DateTime, DateTimeComparator, DateMidnight }
import conf.FootballClient
import akka.util.Duration
import java.util.concurrent.TimeUnit._
import model.Competition
import scala.Some
import java.util.Comparator

trait CompetitionSupport {

  private implicit val dateMidnightOrdering = Ordering.comparatorToOrdering(
    DateTimeComparator.getInstance.asInstanceOf[Comparator[DateMidnight]]
  )

  def competitions: Seq[Competition]

  def withMatchesOn(date: DateMidnight) = competitionSupportWith {
    val competitionsWithMatches = competitions.filter(_.matches.exists(_.isOn(date)))
    competitionsWithMatches.map(c => c.copy(matches = c.matches.filter(_.isOn(date))))
  }

  def withCompetitionFilter(path: String) = competitionSupportWith(
    competitions.filter(_.url == "/football/" + path)
  )

  def withTodaysMatchesAndFutureFixtures = competitionSupportWith {
    val today = new DateMidnight
    competitions.map(c => c.copy(matches = c.matches.filter(m => m.isFixture || m.isOn(today))))
      .filter(_.hasMatches)
  }

  def withTodaysMatchesAndPastResults = competitionSupportWith {
    val today = new DateMidnight
    competitions.map(c => c.copy(matches = c.matches.filter(m => m.isResult || m.isOn(today))))
      .filter(_.hasMatches)
  }

  def withTodaysMatches = competitionSupportWith {
    val today = new DateMidnight
    competitions.map(c => c.copy(matches = c.matches.filter(_.isOn(today)))).filter(_.hasMatches)
  }

  // startDate is inclusive of the days you want
  def nextMatchDates(startDate: DateMidnight, numDays: Int) = competitions
    .flatMap(_.matches)
    .map(_.date.toDateMidnight).distinct
    .sorted
    .filter(_ isAfter startDate.minusDays(1))
    .take(numDays)

  // startDate is inclusive of the days you want
  def previousMatchDates(date: DateMidnight, numDays: Int) = competitions
    .flatMap(_.matches)
    .map(_.date.toDateMidnight)
    .distinct
    .sorted
    .reverse
    .filter(_ isBefore date.plusDays(1))
    .take(numDays)

  private def competitionSupportWith(comps: Seq[Competition]) = new CompetitionSupport {
    def competitions = comps
  }
}

trait Competitions extends CompetitionSupport with AkkaSupport with Logging {

  private implicit val dateOrdering = Ordering.comparatorToOrdering(
    DateTimeComparator.getInstance.asInstanceOf[Comparator[DateTime]]
  )

  private var schedules: Seq[Cancellable] = Nil

  val competitionAgents = Seq(

    CompetitionAgent(Competition("500", "/football/championsleague", "Champions League", "Champions League", "European")),
    CompetitionAgent(Competition("510", "/football/uefa-europa-league", "Europa League", "Europa League", "European")),

    CompetitionAgent(Competition("100", "/football/premierleague", "Premier League", "Premier League", "English")),
    CompetitionAgent(Competition("101", "/football/championship", "Championship", "Championship", "English")),
    CompetitionAgent(Competition("102", "/football/leagueonefootball", "League One", "League One", "English")),
    CompetitionAgent(Competition("103", "/football/leaguetwofootball", "League Two", "League Two", "English")),
    CompetitionAgent(Competition("127", "/football/fa-cup", "FA Cup", "FA Cup", "English")),

    CompetitionAgent(Competition("120", "/football/scottishpremierleague", "Scottish Premier League", "Scottish Premier League", "Scottish")),
    CompetitionAgent(Competition("121", "/football/scottish-division-one", "Scottish Division One", "Scottish Division One", "Scottish")),
    CompetitionAgent(Competition("122", "/football/scottish-division-two", "Scottish Division Two", "Scottish Division Two", "Scottish")),
    CompetitionAgent(Competition("123", "/football/scottish-division-three", "Scottish Division Three", "Scottish Division Three", "Scottish")),

    CompetitionAgent(Competition("301", "/football/capital-one-cup", "Capital One Cup", "Capital One Cup", "English")),
    CompetitionAgent(Competition("213", "/football/community-shield", "Community Shield", "Community Shield", "English"))
  )

  override def competitions = competitionAgents.map { agent =>

    val results = agent.results

    //results trump live games
    val resultsWithLiveGames = agent.liveMatches.filterNot(g => results.exists(_.id == g.id)) ++ results

    //results and live games trump fixtures
    val allGames = agent.fixtures.filterNot(f => resultsWithLiveGames.exists(_.id == f.id)) ++ resultsWithLiveGames

    agent.competition.copy(matches = allGames.sortBy(_.date))
  }

  def refreshAgent(agent: CompetitionAgent) = agent.refresh()

  //one http call updates all competitions
  def refreshCompetitionData() = FootballClient.competitions.foreach { season =>
    log.info("Refreshing competition data")
    competitionAgents.find(_.competition.id == season.id).foreach { agent =>
      agent.update(agent.competition.copy(startDate = Some(season.startDate)))
    }
  }

  //one http call updates all competitions
  def refreshLiveMatches() {
    val liveMatches = FootballClient.matchDay(DateMidnight.now).filter(_.isLive)
    competitionAgents.foreach { agent =>
      val competitionMatches = liveMatches.filter(_.competition.exists(_.id == agent.competition.id))
      log.info("found %s live matches for competition %s".format(competitionMatches.size, agent.competition.fullName))
      agent.updateLiveMatches(competitionMatches)
    }
  }

  def startup() {
    import play_akka.scheduler._
    schedules = every(Duration(10, SECONDS), initialDelay = Duration(1, SECONDS)) { refreshLiveMatches() } ::
      every(Duration(5, MINUTES), initialDelay = Duration(1, SECONDS)) { refreshCompetitionData() } ::
      competitionAgents.zipWithIndex.toList.map {
        case (agent, index) =>
          //stagger fixtures and results refreshes to avoid timeouts
          every(Duration(5, MINUTES), initialDelay = Duration(5 + index, SECONDS)) { refreshAgent(agent) }
      }
  }

  def shutDown() {
    schedules.foreach(_.cancel())
    competitionAgents.foreach(_.shutdown())
  }

  def warmup() {
    refreshCompetitionData()
    competitionAgents.foreach(_.refresh())
    competitionAgents.foreach(_.await())
  }
}

object Competitions extends Competitions