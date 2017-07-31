package org.activiti.web.simple.webapp.model;

import java.util.List;

import javax.persistence.ManyToMany;

import org.activiti.web.simple.webapp.model.example.User;

@SuppressWarnings("serial")
public class Group implements org.activiti.engine.identity.Group {

	private String id;
	private String name;
	private String type;
	
	private List<User> actIdUsers;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	//bi-directional many-to-many association to User
    @ManyToMany(mappedBy = "actIdGroups")
    public List<User> getActIdUsers() {
        return this.actIdUsers;
    }

    public void setActIdUsers(List<User> actIdUsers) {
        this.actIdUsers = actIdUsers;
    }

}