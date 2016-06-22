package edu.uci.ics.cloudberry.noah.crawler;


import com.crawljax.browser.EmbeddedBrowser.BrowserType;

import com.crawljax.condition.UrlCondition;
import com.crawljax.core.CrawljaxException;
import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration.CrawljaxConfigurationBuilder;
import com.crawljax.plugins.crawloverview.CrawlOverview;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class JarRunner {
	static final String MISSING_ARGUMENT_MESSAGE =
	        "Missing required argument URL and/or output folder.";

	private final ParameterInterpeter options;

	private final CrawljaxConfiguration config;

	/**
	 * Main executable method of Crawljax CLI.
	 *
	 * @param args
	 *            the argument
	 */
	public static void main(String[] args) {
		try {
			JarRunner runner = new JarRunner(args);
			runner.runIfConfigured();
		} catch (NumberFormatException e) {
			System.err.println("Could not parse number " + e.getMessage());
			System.exit(1);
		} catch (RuntimeException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	@VisibleForTesting
	JarRunner(String args[]) {
		try {
			this.options = new ParameterInterpeter(args);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		if (options.requestsVersion()) {
			System.out.println(getCrawljaxVersion());
			this.config = null;
		} else if (options.necessaryArgsProvided()) {
			String url = options.getUrl();
			String outputDir = options.getOutputDir();
			//configureLogging();
			this.config = readConfig(url, outputDir);
		} else {
			if (!options.requestsHelp()) {
				System.out.println(MISSING_ARGUMENT_MESSAGE);
			}
			options.printHelp();
			this.config = null;
		}
	}

	private String getCrawljaxVersion() {
		try {
			return Resources
			        .toString(JarRunner.class.getResource("/project.version"), Charsets.UTF_8);
		} catch (IOException e) {
			throw new CrawljaxException(e.getMessage(), e);
		}
	}


	private CrawljaxConfiguration readConfig(String urlValue, String outputDir) {
		CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(urlValue);

		builder.setOutputDirectory(new File(outputDir));

		BrowserType browser = BrowserType.FIREFOX;
		/* Configuration for www.promedmail.org*/

		builder.crawlRules().addCrawlCondition("promedmail only", new UrlCondition("http://www.promedmail.org/"));
		builder.crawlRules().insertRandomDataInInputForms(false);
		builder.crawlRules().dontClick("div").withAttribute("id","header");
		builder.crawlRules().dontClick("div").withAttribute("class","navbar");
		builder.crawlRules().dontClick("table").withAttribute("class","region_title");
		builder.crawlRules().dontClick("div").withAttribute("class","announcements");

		if (options.specifiesBrowser()) {
			browser = options.getSpecifiedBrowser();
		}

		int browsers = 1;
		if (options.specifiesParallelBrowsers()) {
			browsers = options.getSpecifiedNumberOfBrowsers();
		}
		if (browser == BrowserType.REMOTE) {
			String remoteUrl = options.getSpecifiedRemoteBrowser();
			builder.setBrowserConfig(BrowserConfiguration.remoteConfig(browsers, remoteUrl));
		} else {
			builder.setBrowserConfig(new BrowserConfiguration(browser, browsers));
		}


		if (options.specifiesDepth()) {
			builder.setMaximumDepth(options.getSpecifiedDepth());
		}

		if (options.specifiesMaxStates()) {
			builder.setMaximumStates(options.getMaxStates());
		}

		if (options.requestsCrawlHiddenAnchors()) {
			builder.crawlRules().crawlHiddenAnchors(true);
		}


		configureTimers(builder);

		builder.addPlugin(new CrawlOverview());

		if (options.specifiesClickElements()) {
			builder.crawlRules().click(options.getSpecifiedClickElements());
		}else if(options.specifiesDontClickElements()) {
			;
		}else {
			;
		}

		return builder.build();
	}

	private void configureTimers(CrawljaxConfigurationBuilder builder) {
		if (options.specifiesWaitAfterEvent()) {
			builder.crawlRules().waitAfterEvent(options.getSpecifiedWaitAfterEvent(),
			        TimeUnit.MILLISECONDS);
		}
		if (options.specifiesWaitAfterReload()) {
			builder.crawlRules().waitAfterReloadUrl(options.getSpecifiedWaitAfterReload(),
			        TimeUnit.MILLISECONDS);
		}
	}

	private void runIfConfigured() {
		if (config != null) {
			CrawljaxRunner runner = new CrawljaxRunner(config);
			runner.call();
		}
	}

	@VisibleForTesting
	CrawljaxConfiguration getConfig() {
		return config;
	}
}
