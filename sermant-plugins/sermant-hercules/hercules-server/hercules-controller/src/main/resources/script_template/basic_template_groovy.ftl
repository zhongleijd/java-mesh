import static net.grinder.script.Grinder.grinder
import static org.junit.Assert.*
import static org.hamcrest.Matchers.*
import net.grinder.plugin.http.HTTPRequest
import net.grinder.plugin.http.HTTPPluginControl
import net.grinder.script.GTest
import net.grinder.script.Grinder
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

import java.util.Date
import java.util.List
import java.util.ArrayList

import HTTPClient.Cookie
import HTTPClient.CookieModule
import HTTPClient.HTTPResponse
import HTTPClient.NVPair

@RunWith(GrinderRunner)
<#if options??>
	<#assign options = options?eval>
	<#assign method = options["method"]>
	<#assign headers = options["headers"]>
	<#if options["body"]??>
		<#assign body = options["body"]>
	<#else>
		<#assign params = options["params"]>
	</#if>
	<#assign cookies = options["cookies"]>
</#if>
class TestRunner {

	public static GTest test
	public static HTTPRequest request
	public static NVPair[] headers = []
<#if body??>
	public static String body = "${body?j_string?replace("$", "\\$")}"	
<#else>
	public static NVPair[] params = []
</#if>
	public static Cookie[] cookies = []

	@BeforeProcess
	public static void beforeProcess() {
		HTTPPluginControl.getConnectionDefaults().timeout = 6000
		test = new GTest(1, "${name}")
		request = new HTTPRequest()
	<#if headers?? && headers?size != 0>
		// Set header datas
		List<NVPair> headerList = new ArrayList<NVPair>()
		<#list headers as header>
		headerList.add(new NVPair("${header["name"]?j_string}", "${header["value"]?j_string?replace("$", "\\$")}"))
		</#list>
		headers = headerList.toArray()
	</#if>
	<#if params?? && params?size != 0>
		// Set param datas
		List<NVPair> paramList = new ArrayList<NVPair>()
		<#list params as param>
		paramList.add(new NVPair("${param["name"]?j_string}", "${param["value"]?j_string?replace("$", "\\$")}"))
		</#list>
		params = paramList.toArray()
	</#if>
	<#if cookies?? && cookies?size != 0>
		// Set cookie datas
		List<Cookie> cookieList = new ArrayList<Cookie>()
		<#list cookies as cookie>
		cookieList.add(new Cookie("${cookie["name"]?j_string}", "${cookie["value"]?j_string?replace("$", "\\$")}", "${cookie["domain"]?j_string}", "${cookie["path"]?j_string}", new Date(32503647599000L), false))
		</#list>
		cookies = cookieList.toArray()
	</#if>
		grinder.logger.info("before process.");
	}

	@BeforeThread 
	public void beforeThread() {
		test.record(this, "test")
		grinder.statistics.delayReports=true;
		grinder.logger.info("before thread.");
	}
	
	@Before
	public void before() {
		request.setHeaders(headers)
		cookies.each { CookieModule.addCookie(it, HTTPPluginControl.getThreadHTTPClientContext()) }
		grinder.logger.info("before thread. init headers and cookies");
	}

	@Test
	public void test(){
		HTTPResponse result = request.${method?default("GET")}("${url}", <#if body??>body.getBytes()<#else>params</#if>)

		if (result.statusCode == 301 || result.statusCode == 302) {
			grinder.logger.warn("Warning. The response may not be correct. The response code was {}.", result.statusCode); 
		} else {
			assertThat(result.statusCode, is(200));
		}
	}
}
