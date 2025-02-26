# -*- coding:utf-8 -*-

from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from net.grinder.plugin.http import HTTPRequest
from net.grinder.plugin.http import HTTPPluginControl
from java.util import Date
from HTTPClient import NVPair, Cookie, CookieModule

control = HTTPPluginControl.getConnectionDefaults()
# if you don't want that HTTPRequest follows the redirection, please modify the following option 0.
# control.followRedirects = 1
# if you want to increase the timeout, please modify the following option.
control.timeout = 6000

test1 = Test(1, "${name}")
request1 = HTTPRequest()

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
# Set header datas
headers = [] # Array of NVPair
<#if headers?? && headers?size != 0>
	<#list headers as header>
headers.append(NVPair("${header["name"]?j_string}", "${header["value"]?j_string}"))
	</#list>
</#if>
<#if body??>
# Set form data
body = "${body?j_string}"	# String of form data
<#else>
# Set param datas
params = [] # Array of NVPair
</#if>
<#if params?? && params?size != 0>
	<#list params as param>
params.append(NVPair("${param["name"]?j_string}", "${param["value"]?j_string}"))
	</#list>
</#if>
# Set cookie datas
cookies = [] # Array of Cookie
<#if cookies?? && cookies?size != 0>
	<#list cookies as cookie>
cookies.append(Cookie("${cookie["name"]?j_string}", "${cookie["value"]?j_string}", "${cookie["domain"]?j_string}", "${cookie["path"]?j_string}", Date(32503647599000L), 0))
	</#list>
</#if>

class TestRunner:
	# initlialize a thread 
	def __init__(self):
		test1.record(TestRunner.__call__)
		grinder.statistics.delayReports=True
		pass
	
	def before(self):
		request1.headers = headers
		for c in cookies: CookieModule.addCookie(c, HTTPPluginControl.getThreadHTTPClientContext())

	# test method		
	def __call__(self):
		self.before()
		
		result = request1.${method?default("GET")}("${url}", <#if body??>body<#else>params</#if>)
		
		# You get the message body using the getText() method.
		# if result.getText().find("HELLO WORLD") == -1 :
		#	 raise
			
		# if you want to print out log.. Don't use print keyword. Instead, use following.
		# grinder.logger.info("Hello World")
		
		if result.getStatusCode() == 200 :
			return
		elif result.getStatusCode() in (301, 302) :
			grinder.logger.warn("Warning. The response may not be correct. The response code was %d." %  result.getStatusCode()) 
			return
		else :
			raise
