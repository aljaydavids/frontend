package feed

import common.AkkaSupport
import akka.actor.Cancellable
import org.joda.time.{ DateTime, DateTimeComparator, DateMidnight }
import conf.FootballClient
import akka.util.Duration
import java.util.concurrent.TimeUnit._
import model.Competition
import scala.Some
import java.util.Comparator
import pa.FootballMatch

trait Competitions extends AkkaSupport {

  private implicit val dateOrdering = Ordering.comparatorToOrdering(
    DateTimeComparator.getInstance.asInstanceOf[Comparator[DateTime]]
  )

  private var schedules: Seq[Cancellable] = Nil

  private val competitions = Seq(

    CompetitionAgent(Competition("500", "/football/championsleague", "Champions League", "Champions League")),
    CompetitionAgent(Competition("510", "/football/uefa-europa-league", "Europa League", "Europa League")),

    CompetitionAgent(Competition("100", "/football/premierleague", "Premier League", "Premier League")),
    CompetitionAgent(Competition("101", "/football/championship", "Championship", "Championship")),
    CompetitionAgent(Competition("102", "/football/leagueonefootball", "League One", "League One")),
    CompetitionAgent(Competition("103", "/football/leaguetwofootball", "League Two", "League Two")),
    CompetitionAgent(Competition("127", "/football/fa-cup", "FA Cup", "FA Cup")),

    CompetitionAgent(Competition("120", "/football/scottishpremierleague", "Scottish Premier League", "Scottish Premier League")),
    CompetitionAgent(Competition("121", "/football/scottish-division-one", "Scottish Division One", "Scottish Division One")),
    CompetitionAgent(Competition("122", "/football/scottish-division-two", "Scottish Division Two", "Scottish Division Two")),
    CompetitionAgent(Competition("123", "/football/scottish-division-three", "Scottish Division Three", "Scottish Division Three")),

    CompetitionAgent(Competition("301", "/football/capital-one-cup", "Capital One Cup", "Capital One Cup")),
    CompetitionAgent(Competition("213", "/football/community-shield", "Community Shield", "Community Shield"))
  )

  def withMatchesOn(date: DateMidnight) = competitions.map { competition =>

    val results = competition.resultsOn(date)
    //results trump live games
    val resultsWithLiveGames = competition.liveMatches.filter(_.date.toDateMidnight == date)
      .filterNot(g => results.exists(_.id == g.id)) ++ results
    //results and live games trump fixtures
    val allGames = competition.fixturesOn(date).filterNot(f => resultsWithLiveGames.exists(_.id == f.id)) ++ results

    competition.competition.copy(matches = allGames.sortBy(_.date))
  }.filter(_.matches.nonEmpty)

  def nextFixtureDatesStarting(date: DateMidnight, numDays: Int): Seq[DateMidnight] =
    nextDates(date, numDays, competitions.flatMap(_.fixtures))

  def lastFixtureDatesBefore(date: DateMidnight, numDays: Int): Seq[DateMidnight] =
    previousDates(date, numDays, competitions.flatMap(_.fixtures))

  def lastResultsDatesEnding(date: DateMidnight, numDays: Int): Seq[DateMidnight] =
    previousDates(date, numDays, competitions.flatMap(_.results))

  def nextResultsDatesStarting(date: DateMidnight, numDays: Int): Seq[DateMidnight] =
    nextDates(date, numDays, competitions.flatMap(_.results))

  private def nextDates(date: DateMidnight, numDays: Int, matches: Seq[FootballMatch]) = matches
    .map(_.date.toDateMidnight).distinct
    .sortBy(_.getMillis)
    .filter(_ isAfter date.minusDays(1))
    .take(numDays)

  private def previousDates(date: DateMidnight, numDays: Int, matches: Seq[FootballMatch]) = matches
    .map(_.date.toDateMidnight)
    .distinct
    .sortBy(_.getMillis)
    .reverse
    .filter(_ isBefore date.plusDays(1))
    .take(numDays)

  private def refreshCompetitionData() = FootballClient.competitions.foreach { season =>
    competitions.find(_.competition.id == season.id).foreach { agent =>
      agent.update(agent.competition.copy(startDate = Some(season.startDate)))
      agent.refresh()
    }
  }

  def refresh() = competitions.foreach(_.refresh())

  def startup() {
    import play_akka.scheduler._
    schedules = every(Duration(5, MINUTES)) { refreshCompetitionData() } ::
      every(Duration(2, MINUTES), initialDelay = Duration(5, SECONDS)) { refresh() } ::
      every(Duration(10, SECONDS)) { competitions.foreach(_.refreshLiveMatches()) } ::
      Nil
  }

  def shutDown() {
    schedules.foreach(_.cancel())
    competitions.foreach(_.shutdown())
  }

  def warmup() {
    refreshCompetitionData()
    refresh()
    competitions.foreach(_.await())
  }
}

object Competitions extends Competitions