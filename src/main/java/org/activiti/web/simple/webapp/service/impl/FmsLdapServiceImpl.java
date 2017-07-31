package org.activiti.web.simple.webapp.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.activiti.engine.identity.User;
import org.activiti.web.simple.webapp.service.AccountService;
import org.activiti.web.simple.webapp.service.FmsLdapService;
import org.springframework.stereotype.Service;

@Service("fmsLdapServiceImpl")
public class FmsLdapServiceImpl implements FmsLdapService {

	@Resource(name = "accountServiceImpl")
	private AccountService accountService;

	@Override
	public List<String> findDeptLeaders(String employee) {
		// TODO Auto-generated method stub
		// 返回待办人集合
		List<String> userList = new ArrayList<String>();
		User user = accountService.getUserById(employee);
		userList.add(user.getId());
		return userList;
	}

	@Override
	public List<String> findHrOperators(String employee) {
		// TODO Auto-generated method stub
		// 返回待办人集合
		List<String> userList = new ArrayList<String>();
		User user = accountService.getUserById(employee);
		userList.add(user.getId());
		return userList;
	}

	@Override
	public List<String> findLeaders(String employee) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> findFinances(String employee) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> findCashiers(String employee) {
		// TODO Auto-generated method stub
		return null;
	}

}
