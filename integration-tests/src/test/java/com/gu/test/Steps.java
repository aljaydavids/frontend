package com.gu.test;

import java.io.File;
import java.util.List;

import junit.framework.Assert;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import cucumber.annotation.en.Given;
import cucumber.annotation.en.Then;
import cucumber.annotation.en.When;

public class Steps {

    private final SharedDriver webDriver;
    
    protected String sectionFrontUrl = "/sport";
    protected String articleUrl = "/football/2012/oct/15/steven-gerrard-england-poland-generation";
    
    public Steps(SharedDriver webDriver) {
        this.webDriver = webDriver;
    }
	
	@Given("^I visit the network front")
	public void i_visit_the_network_front() throws Throwable {
		webDriver.open("/");
	}
	
	@Given("^I visit a section front")
	public void i_visit_a_section_front() throws Throwable {
		webDriver.open(sectionFrontUrl);
	}
	
	@Given("^I visit an article")
	public void i_visit_an_article() throws Throwable {
		webDriver.open(articleUrl);
	}

	@When("^I refresh the page$")
	public void I_refresh_the_page() throws Throwable {
	    // refresh the page
		webDriver.navigate().refresh();
	}
	
	@Then("^the \"(Top stories|Sections)\" tab is (hidden|shown)$")
	public void tab_is(String tabName, String tabState) throws Throwable {
		String tabId = tabName.toLowerCase().replace(" ", "") + "-control-header";
	    WebElement tab = webDriver.findElement(By.id(tabId));
	    // confirm element is shown/hidden
	    Assert.assertEquals(tabState.equals("shown"), tab.isDisplayed());
	}
	
	@When("^I visit the (.*) jasmine test runner$")
	public void I_visit_the_jasmine_test_runner(String project) throws Throwable {
		File currentDir = new File(".");
		System.out.println(currentDir.getCanonicalPath());
		// open the appropriate runner 
		webDriver.get(
			"file:///" + currentDir.getCanonicalPath() + "/../" + project + "/test/assets/javascripts/runner.html"
		);
		// confirm we're on the correct page
		Assert.assertTrue(webDriver.getTitle().contains("Jasmine Spec Runner"));
	}

	@Then("^all the jasmine tests pass$")
	public void all_the_tests_pass() throws Throwable {
		// store the number of tests
		int numOfTests = webDriver.findElements(By.cssSelector("#tests a")).size();
		boolean testFailure = false;
		// run each test
		for (int i = 0; i < numOfTests; i++) {
			WebElement test = webDriver.findElements(By.cssSelector("#tests a")).get(i);
			String testName = test.getText();
			test.click();
			List<WebElement> alertBar = webDriver.findElements(By.cssSelector("span.failingAlert.bar"));
			if (alertBar.size() != 0) {
				System.out.println(testName + " - " + alertBar.get(0).getText());
				for (WebElement error : webDriver.findElements(By.className("specDetail"))) {
					System.out.println("  " + error.findElement(By.className("description")).getText());
					for (WebElement errorMsg : error.findElements(By.className("resultMessage"))) {
						System.out.println(" > " + errorMsg.getText());
					}
				}
				testFailure = true;
			}
		}
		
		Assert.assertFalse(testFailure);
	}
	
}