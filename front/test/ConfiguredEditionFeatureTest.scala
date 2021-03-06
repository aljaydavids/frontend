package test

import org.scalatest.{ GivenWhenThen, FeatureSpec }

import controllers.front.ConfiguredEdition
import model.TrailblockDescription
import org.scalatest.matchers.ShouldMatchers

class ConfiguredEditionFeatureTest extends FeatureSpec with GivenWhenThen with ShouldMatchers {

  feature("Configured front") {

    scenario("Load front configuration for UK edition") {

      given("I visit the Network Front")
      and("I am on the UK edition")
      Fake {
        val front = new ConfiguredEdition {
          override def edition = "UK"
          override val configUrl = "http://s3-eu-west-1.amazonaws.com/aws-frontend-store/TMC/config/front-test.json"
        }

        front.refresh()
        front.warmup()

        then("I should see the configured feature trailblock")
        front.configuredTrailblocks.map(_.description) should be(Seq(TrailblockDescription("politics", "Politics", 3)))
      }
    }

    scenario("Load front configuration for US edition") {

      given("I visit the Network Front")
      and("I am on the US edition")
      Fake {
        val front = new ConfiguredEdition {
          override def edition = "US"
          override val configUrl = "http://s3-eu-west-1.amazonaws.com/aws-frontend-store/TMC/config/front-test.json"
        }

        front.refresh()
        front.warmup()

        then("I should see the configured feature trailblock")
        front.configuredTrailblocks.map(_.description) should be(Seq(TrailblockDescription("world/iraq", "Iraq", 3)))
      }
    }

    scenario("Survive loading bad configuration") {

      given("I visit the Network Front")
      and("the feature trailblock has broken confiuration")
      Fake {
        val front = new ConfiguredEdition {
          override def edition = "US"
          override val configUrl = "http://s3-eu-west-1.amazonaws.com/aws-frontend-store/TMC/config/front-bad-does-not-exist.json"
        }

        front.refresh()
        front.warmup()

        then("I the feature trailblock should collapse")
        front.configuredTrailblocks.map(_.description) should be(Nil)
      }
    }
  }
}
