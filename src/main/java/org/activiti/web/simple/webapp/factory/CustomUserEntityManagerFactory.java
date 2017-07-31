package org.activiti.web.simple.webapp.factory;

import javax.annotation.Resource;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.entity.UserIdentityManager;
import org.activiti.web.simple.webapp.service.impl.CustomUserManager;
import org.springframework.stereotype.Service;

/**
 * 自定义的Activiti用户会话工厂
 *
 * @author xuzw
 */
@Service
public class CustomUserEntityManagerFactory implements SessionFactory {
	@Resource
	private CustomUserManager customUserManager;

	@Override
	public Class<?> getSessionType() {
		// 返回原始的UserManager类型
		return UserIdentityManager.class;
	}

	@Override
	public Session openSession() {
		// 返回自定义的UserManager实例
		return customUserManager;
	}
}