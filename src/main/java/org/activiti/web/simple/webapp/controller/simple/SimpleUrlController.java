package org.activiti.web.simple.webapp.controller.simple;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * 该控制器在spring容器中标识为SimpleUrlController
 * 采用了org.springframework.web.servlet.handler.SimpleUrlHandlerMapping来进行标配
 * 详见spring-mvc-servlet.xml中 <bean
 * class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
 * 中<prop key="/SimpleUrl">SimpleUrlController</prop>的配置 下面各方法的请求路径为
 * /simpleUrl/jsp /simpleUrl/velocity /simpleUrl/freemarker
 * 
 * @author xuzw
 *
 */
@Controller(value = "SimpleUrlController")
@RequestMapping(value = "/simpleurl")
public class SimpleUrlController {

	@RequestMapping(value = "/jsp")
	public ModelAndView jsp(HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) throws Exception {
		ModelAndView modelAndView = new ModelAndView("welcome");
		modelAndView.addObject("message", "Hello,SpringMVC!");
		return modelAndView;
	}

	@RequestMapping(value = "/velocity")
	public ModelAndView velocity(HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) throws Exception {
		ModelAndView modelAndView = new ModelAndView("velocity");
		modelAndView.addObject("message", "Hello,SpringMVC!");
		return modelAndView;
	}

	@RequestMapping(value = "/freemarker")
	public ModelAndView freemarker(HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) throws Exception {
		ModelAndView modelAndView = new ModelAndView("freemarker");
		modelAndView.addObject("message", "Hello,SpringMVC!");
		return modelAndView;
	}

	/**
	 * 从地址栏接受参数 /simpleUrl/showUser/admin/admin name为admin pwd为admin
	 * 请求转发到showuser.jsp
	 * 
	 * @param username
	 * @param password
	 * @param map
	 * @return
	 */
	@RequestMapping(value = "/showUser/{name}/{pwd}")
	public String showUserName(@PathVariable(value = "name") String username,
			@PathVariable(value = "pwd") String password, ModelMap map) {
		try {
			map.addAttribute("username",
					new String(username.getBytes("iso-8859-1"), "utf-8"));
			map.addAttribute("pwd", password);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "showuser";
	}

}