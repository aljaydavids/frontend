package test

import org.scalatest.{ FeatureSpec, GivenWhenThen }
import org.scalatest.matchers.ShouldMatchers
import collection.JavaConversions._

class CompetitionsResultsFeatureTest extends FeatureSpec with GivenWhenThen with ShouldMatchers {

  feature("Football Results") {

    ignore("Visit the results page") {

      given("I visit the results page")

      //the url /football/results is based on the current day
      //this just checks it loads
      HtmlUnit("/football/results") { browser =>
        import browser._
        findFirst("h1").getText should be("All results")
      }

      //A dated url will give us a fixed set of results we can assert against
      HtmlUnit("/football/premierleague/results/2012/oct/07") { browser =>
        import browser._
        then("I should see results for today")

        findFirst(".competitions-date").getText should be("Sunday 7 October 2012")

        val fixture = findFirst(".matches").findFirst(".match-desc")
        fixture.findFirst(".home").getText should be("Southampton")
        fixture.findFirst(".away").getText should be("Fulham")
        fixture.findFirst(".result").getText should be("2 - 2")
        findFirst(".status").getText should include("FT")

        and("I should see results for the past")
        $(".competitions-date").getTexts should contain("Saturday 6 October 2012")
        $(".competitions-date").getTexts should contain("Monday 1 October 2012")
      }
    }

    ignore("Next results") {
      given("I am on the results page")
      //A dated url will give us a fixed set of results we can assert against
      HtmlUnit("/football/premierleague/results/2012/oct/07") { browser =>
        import browser._
        when("I click the 'next' link")

        findFirst("[data-link-name=next]").click()
        browser.await()

        then("I should navigate to the next set of results")
        findFirst(".competitions-date").getText should be("Saturday 22 December 2012")
      }
    }

    ignore("Link tracking") {
      given("I visit the results page")
      HtmlUnit("/football/premierleague/results/2012/oct/07") { browser =>
        import browser._
        then("any links I click should be tracked")
        $("a").filter(link => !Option(link.getAttribute("data-link-name")).isDefined).foreach { link =>
          fail("Link with text %s has no data-link-name".format(link.getText))
        }
      }
    }
  }
}
