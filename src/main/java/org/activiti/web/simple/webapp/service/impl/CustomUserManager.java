package org.activiti.web.simple.webapp.service.impl;

import java.util.List;

import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.UserQueryImpl;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.impl.persistence.entity.UserEntityManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 自定义用户管理器
 * 
 * @author xuzw
 *
 */
@Service("customUserManager")
public class CustomUserManager extends UserEntityManager {

	//private Logger logger = LoggerFactory.getLogger(getClass());

	/*@Override
	public User findUserById(String userId) {
		User user = new UserEntity();
		// 引入Uap系统客户端包，关联进行查询
		BaseDto baseDto = RoomyUapUserUtils.getUser(userId);
		if (baseDto.getRemoteStatus() == RemoteStatus.SUCCESS) {
			UserDto userDto = (UserDto) baseDto;
			user.setId(userDto.getUserName());
			user.setFirstName(userDto.getName());
			user.setLastName(userDto.getName());
		} else {
			user = null;
			logger.info("用户名:" + userId + ";无法获取用户信息");
		}
		return user;
	}

	@Override
	public List<Group> findGroupsByUser(String userId) {
		// ……
		return list;
	}

	@Override
	public Boolean checkPassword(String userId, String password) {
		// ……
		return Boolean.valueOf(true);
	}

	@Override
	public List<User> findUserByQueryCriteria(UserQueryImpl query, Page page) {
		// ……
		return list;
	}

	@Override
	public long findUserCountByQueryCriteria(UserQueryImpl query) {
		// ……
	}*/
}